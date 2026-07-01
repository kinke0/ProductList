#!/usr/bin/env python3
"""
ProductList 部署脚本
====================
本地编译前后端 → 上传制品(jar+dist+数据) → 远程 Docker 容器挂载运行
镜像只含运行环境(jre+nginx)，业务文件全部通过卷挂载，更新时无需重建镜像

用法:
  # 首次部署（构建运行环境镜像 + 上传全部制品 + 启动容器）
  python deploy.py --init

  # 日常更新（本地编译 → 上传 jar+dist → 重启容器）
  python deploy.py

  # 只更新数据文件（不上传 jar/dist）
  python deploy.py --update-data

  # 跳过本地编译，直接上传已有制品
  python deploy.py --skip-build

    # 交互菜单模式（DOS 风格）
    python deploy.py --menu
"""

from __future__ import annotations

import argparse
import gzip
import os
import platform
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path, PurePosixPath

try:
    import paramiko
except ImportError as exc:
    print("Missing dependency: paramiko")
    print("Install with: pip install paramiko")
    raise SystemExit(1) from exc


# ======================= 默认配置 =======================
DEFAULT_HOST = "10.104.6.53"
DEFAULT_USER = "root"
DEFAULT_PASSWORD = "his.123456"
DEFAULT_IMAGE = "product-list:1.0"
DEFAULT_CONTAINER = "product-list-app"
DEFAULT_REMOTE_DIR = "/opt/productlist"
DEFAULT_PORT = 80

# 本地编译产物路径
DEFAULT_BACKEND_JAR_GLOB = "target/*.jar"          # mvn package 输出
DEFAULT_FRONTEND_DIST = "frontend/dist"            # npm run build 输出
DEFAULT_UPLOADS_DIR = "uploads"
DEFAULT_DOCS_DIR = "generated-docs"


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Deploy ProductList (本地编译 + 远程挂载)")

    # 连接
    p.add_argument("--host", default=DEFAULT_HOST)
    p.add_argument("--user", default=DEFAULT_USER)
    p.add_argument("--password", default=DEFAULT_PASSWORD)

    # 远程
    p.add_argument("--remote-dir", default=DEFAULT_REMOTE_DIR)
    p.add_argument("--image", default=DEFAULT_IMAGE)
    p.add_argument("--container", default=DEFAULT_CONTAINER)
    p.add_argument("--port", type=int, default=DEFAULT_PORT)

    # 本地编译
    p.add_argument("--skip-build", action="store_true",
                   help="跳过本地 mvn/npm 编译，直接上传已有制品")
    p.add_argument("--backend-jar", default=None,
                   help="指定后端 jar 文件路径 (默认自动查找 target/*.jar)")

    # 模式
    p.add_argument("--init", action="store_true",
                   help="首次部署：构建运行环境镜像 + 上传全部制品 + 启动容器")
    p.add_argument("--update-data", action="store_true",
                   help="只更新数据文件 (uploads/docs)，不更新 jar/dist")
    p.add_argument("--data-items", default="all", choices=["all", "uploads", "docs"],
                   help="--update-data 时指定更新的数据项 (默认: all)")
    p.add_argument("--version", action="store_true", help="只更新版本号文件 VERSION.md")
    p.add_argument("--uploads", action="store_true", help="只更新附件目录")
    p.add_argument("--docs", action="store_true", help="只更新文档目录")
    p.add_argument("--menu", action="store_true",
                   help="进入交互式菜单模式，手动选择部署动作")

    return p.parse_args()


# ======================= 跨平台命令执行 =======================
IS_WINDOWS = platform.system() == "Windows"


def run_cmd(cmd: list[str], **kwargs) -> subprocess.CompletedProcess:
    """跨平台执行命令，Windows 上用 shell=True 才能找到 .cmd/.bat"""
    if IS_WINDOWS:
        return subprocess.run(" ".join(cmd), shell=True, **kwargs)
    else:
        return subprocess.run(cmd, **kwargs)


def popen_cmd(cmd: list[str], **kwargs) -> subprocess.Popen:
    """跨平台 Popen"""
    if IS_WINDOWS:
        return subprocess.Popen(" ".join(cmd), shell=True, **kwargs)
    else:
        return subprocess.Popen(cmd, **kwargs)


# ======================= SSH 客户端 =======================
class Remote:
    def __init__(self, host, user, password):
        self.host = host
        self.user = user
        self.password = password
        self._client: paramiko.SSHClient | None = None

    def __enter__(self):
        c = paramiko.SSHClient()
        c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        c.connect(self.host, 22, self.user, self.password)
        self._client = c
        return self

    def __exit__(self, *a):
        if self._client:
            self._client.close()

    def run(self, cmd: str) -> str:
        _, stdout, stderr = self._client.exec_command(cmd)
        out = stdout.read().decode("utf-8", errors="replace")
        err = stderr.read().decode("utf-8", errors="replace")
        code = stdout.channel.recv_exit_status()
        if code != 0:
            raise RuntimeError(f"远程命令失败({code}): {cmd}\n{err or out}")
        return out.strip()

    def _ensure_remote_dir(self, sftp, remote_dir: str):
        remote_path = PurePosixPath(remote_dir)
        if str(remote_path) in {"", ".", "/"}:
            return

        parts: list[str] = []
        current = remote_path
        while str(current) not in {"", ".", "/"}:
            parts.append(str(current))
            parent = current.parent
            if parent == current:
                break
            current = parent

        for directory in reversed(parts):
            try:
                sftp.stat(directory)
            except IOError:
                sftp.mkdir(directory)

    def upload_file(self, local: Path, remote_path: str):
        print(f"  上传: {local} -> {remote_path}")
        sftp = self._client.open_sftp()
        try:
            self._ensure_remote_dir(sftp, str(PurePosixPath(remote_path).parent))
            sftp.put(str(local), remote_path)
        finally:
            sftp.close()

    def upload_dir(self, local_dir: Path, remote_base: str):
        """递归上传目录"""
        files = [f for f in local_dir.rglob("*") if f.is_file()]
        total = len(files)
        print(f"  上传目录: {local_dir} -> {remote_base} (共 {total} 个文件)")

        sftp = self._client.open_sftp()
        try:
            self._ensure_remote_dir(sftp, remote_base)
            for i, f in enumerate(files, 1):
                rel = f.relative_to(local_dir)
                rp = f"{remote_base}/{rel}".replace("\\", "/")
                self._ensure_remote_dir(sftp, str(PurePosixPath(rp).parent))
                print(f"    [{i}/{total}] {rel}")
                sftp.put(str(f), rp)
        finally:
            sftp.close()


# ======================= 本地构建 =======================
def local_build_backend(repo_root: Path) -> Path:
    """本地 mvn package, 返回 jar 路径"""
    print("[本地] Maven 编译后端...")
    run_cmd(
        ["mvn", "package", "-DskipTests", "-Dmaven.test.skip=true"],
        cwd=str(repo_root), check=True
    )
    jars = sorted(repo_root.glob("target/*.jar"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not jars:
        raise FileNotFoundError("未找到编译产物 target/*.jar")
    jar = jars[0]
    print(f"  [OK] {jar.name} ({jar.stat().st_size / 1024 / 1024:.1f} MB)")
    return jar


def local_build_frontend(repo_root: Path) -> Path:
    """本地 npm run build, 返回 dist 目录路径"""
    frontend_dir = repo_root / "frontend"
    vite_executable = frontend_dir / "node_modules" / ".bin" / ("vite.cmd" if IS_WINDOWS else "vite")
    if not vite_executable.exists():
        print("[本地] 检测到前端依赖未安装，先执行 npm install...")
        run_cmd(["npm", "install"], cwd=str(frontend_dir), check=True)
    print("[本地] NPM 编译前端...")
    run_cmd(["npm", "run", "build"], cwd=str(frontend_dir), check=True)
    dist = frontend_dir / "dist"
    if not dist.exists():
        raise FileNotFoundError(f"前端编译产物不存在: {dist}")
    print(f"  [OK] dist/ 已生成")
    return dist


# ======================= 首次初始化 =======================
def do_init(args: argparse.Namespace, repo_root: Path):
    """首次部署: 构建运行时镜像 + 上传全部制品 + 启动容器"""
    remote = args.remote_dir
    data_dir = f"{remote}/data"

    # 1. 本地编译
    if args.skip_build:
        print("[本地] 跳过编译，使用已有制品")
        jar = args.backend_jar and Path(args.backend_jar)
        if not jar:
            jars = sorted(repo_root.glob("target/*.jar"), key=lambda p: p.stat().st_mtime, reverse=True)
            jar = jars[0] if jars else None
        if not jar or not jar.exists():
            raise FileNotFoundError("未找到 jar 文件，请先编译或指定 --backend-jar")
        print(f"  使用 jar: {jar.name}")
        dist = repo_root / "frontend" / "dist"
        if not dist.exists():
            raise FileNotFoundError("未找到前端 dist 目录，请先编译")
    else:
        jar = local_build_backend(repo_root)
        dist = local_build_frontend(repo_root)

    # 2. 构建运行时 Docker 镜像
    print("[本地] 构建运行环境 Docker 镜像 (x86_64)...")
    run_cmd(
        ["docker", "build", "--platform", "linux/amd64", "-t", args.image, "-f", "Dockerfile", "."],
        cwd=str(repo_root), check=True
    )
    print(f"  [OK] 镜像 {args.image} 构建完成")

    # 3. 导出镜像
    tar_path = repo_root / "product-list-runtime.tar.gz"
    print(f"[本地] 导出镜像 -> {tar_path}")
    with tempfile.NamedTemporaryFile(suffix=".tar", delete=False) as tmp:
        proc = popen_cmd(["docker", "save", args.image], stdout=tmp)
        proc.wait()
        tmp_path = Path(tmp.name)
    # 用 Python gzip 压缩（跨平台，不依赖系统 gzip 命令）
    print(f"[本地] 压缩镜像...")
    with open(tmp_path, "rb") as f_in, gzip.open(tar_path, "wb", compresslevel=6) as f_out:
        shutil.copyfileobj(f_in, f_out)
    tmp_path.unlink()

    # 4. 上传
    with Remote(args.host, args.user, args.password) as r:
        r.run(f"mkdir -p {remote} {data_dir}/uploads {data_dir}/docs {remote}/dist")

        print(f"\n[远程] 上传制品到 {args.host}...")
        r.upload_file(tar_path, f"{remote}/runtime.tar.gz")
        r.upload_file(jar, f"{remote}/app.jar")
        r.upload_file(repo_root / "VERSION.md", f"{remote}/VERSION.md")
        r.upload_dir(dist, f"{remote}/dist")

        # 数据文件 (不再上传 SQLite 数据库文件，数据库由 PostgreSQL 容器管理)
        for d in [DEFAULT_UPLOADS_DIR, DEFAULT_DOCS_DIR]:
            src = repo_root / d
            if src.exists() and any(src.iterdir()):
                subdir = "uploads" if d == DEFAULT_UPLOADS_DIR else "docs"
                print(f"  清理并上传 {subdir}...")
                r.run(f"rm -rf {data_dir}/{subdir} && mkdir -p {data_dir}/{subdir}")
                r.upload_dir(src, f"{data_dir}/{subdir}")

        # 5. 加载镜像并启动
        print("\n[远程] 加载镜像并启动容器...")
        r.run(f"gzip -dc {remote}/runtime.tar.gz | docker load")
        recreate_container(r, args)

        time.sleep(5)
        # 验证
        print("\n[验证] 检查服务...")
        try:
            r.run(f"curl -fsS http://127.0.0.1:{args.port}/ && echo ' [OK]'")
        except RuntimeError:
            print("  [WARN] 服务可能还在启动中，请稍后手动检查")

    # 清理
    tar_path.unlink(missing_ok=True)
    print("\n[完成] 首次部署完成!")


# ======================= 日常更新 (jar+dist) =======================
def do_deploy(args: argparse.Namespace, repo_root: Path):
    """日常更新: 编译 → 上传 jar+dist → 重启容器"""
    remote = args.remote_dir

    # 1. 本地编译
    if args.skip_build:
        print("[本地] 跳过编译，使用已有制品")
        jar = args.backend_jar and Path(args.backend_jar)
        if not jar:
            jars = sorted(repo_root.glob("target/*.jar"), key=lambda p: p.stat().st_mtime, reverse=True)
            jar = jars[0] if jars else None
        if not jar or not jar.exists():
            raise FileNotFoundError("未找到 jar 文件，请先编译或指定 --backend-jar")
        print(f"  使用 jar: {jar}")
    else:
        jar = local_build_backend(repo_root)
        local_build_frontend(repo_root)

    dist = repo_root / "frontend" / "dist"

    # 2. 上传 & 重启
    with Remote(args.host, args.user, args.password) as r:
        r.run(f"mkdir -p {remote}/dist")

        print(f"\n[远程] 上传制品到 {args.host}...")
        r.upload_file(jar, f"{remote}/app.jar")
        r.upload_file(repo_root / "VERSION.md", f"{remote}/VERSION.md")
        r.upload_dir(dist, f"{remote}/dist")

        print("\n[远程] 重启容器...")
        recreate_container(r, args)

        print("\n[完成] 更新完成!")


# ======================= 只更新数据 =======================
def do_update_version(args: argparse.Namespace, repo_root: Path):
    """只更新 VERSION.md"""
    remote = args.remote_dir
    with Remote(args.host, args.user, args.password) as r:
        print(f"[远程] 更新版本文件...")
        r.upload_file(repo_root / "VERSION.md", f"{remote}/VERSION.md")
        print("[远程] 重启容器以应用新挂载...")
        recreate_container(r, args)
        print("\n[完成] 版本号更新完成!")


def do_update_data(args: argparse.Namespace, repo_root: Path):
    """只更新数据文件，不更新 jar/dist"""
    remote = args.remote_dir
    data_dir = f"{remote}/data"

    # 确定需要更新的项目
    items = []
    if args.data_items == "all":
        items = ["uploads", "docs"]
    else:
        items = [args.data_items]

    # 如果有命令行 flag，增加对应项 (CLI 模式)
    if not args.menu:
        cli_items = []
        if getattr(args, "uploads", False): cli_items.append("uploads")
        if getattr(args, "docs", False): cli_items.append("docs")
        if cli_items:
            items = cli_items

    with Remote(args.host, args.user, args.password) as r:
        r.run(f"mkdir -p {data_dir}/uploads {data_dir}/docs")

        updated = False

        # uploads
        if "uploads" in items:
            src = repo_root / DEFAULT_UPLOADS_DIR
            if src.exists() and any(src.iterdir()):
                print(f"[远程] 清理并上传 uploads...")
                r.run(f"rm -rf {data_dir}/uploads && mkdir -p {data_dir}/uploads")
                r.upload_dir(src, f"{data_dir}/uploads")
                updated = True

        # docs
        if "docs" in items:
            src = repo_root / DEFAULT_DOCS_DIR
            if src.exists() and any(src.iterdir()):
                print(f"[远程] 清理并上传 docs...")
                r.run(f"rm -rf {data_dir}/docs && mkdir -p {data_dir}/docs")
                r.upload_dir(src, f"{data_dir}/docs")
                updated = True

        if not updated:
            print("[远程] 没有需要上传的数据文件")
            return

        recreate_container(r, args)
        print("\n[完成] 数据更新完成!")


# ======================= 工具函数 =======================
def recreate_container(r: Remote, args: argparse.Namespace):
    """重建远程容器 (rm + run)"""
    remote = args.remote_dir
    data_dir = f"{remote}/data"

    print(f"  正在移除旧容器 {args.container}...")
    r.run(f"docker rm -f {args.container} 2>/dev/null || true")

    print(f"  正在启动新容器...")
    run_cmd_str = (
        f"docker run -d --name {args.container} "
        f"--restart unless-stopped -p {args.port}:80 "
        f"--link productlist-pg:productlist-pg "
        f"-e SPRING_PROFILES_ACTIVE=prod "
        f"-e SPRING_DATASOURCE_URL=jdbc:postgresql://productlist-pg:5432/productlist "
        f"-e PG_PASSWORD=productlist123 "
        f"-v {remote}/app.jar:/app/app.jar "
        f"-v {remote}/VERSION.md:/app/VERSION.md "
        f"-v {remote}/dist:/usr/share/nginx/html "
        f"-v {data_dir}/uploads:/app/uploads "
        f"-v {data_dir}/docs:/app/generated-docs "
        f"{args.image}"
    )
    r.run(run_cmd_str)

    print("  等待容器启动...")
    time.sleep(6)

    status = r.run(f"docker ps --filter name={args.container} --format '{{{{.Status}}}}'")
    print(f"  容器状态: {status}")

    try:
        r.run(f"curl -fsS http://127.0.0.1:{args.port}/ && echo '  服务响应 [OK]'")
    except RuntimeError:
        print("  [WARN] 服务响应异常，请稍后手动验证")


def prompt_choice(title: str, options: list[tuple[str, str]], default: str | None = None) -> str:
    print()
    print(title)
    for key, label in options:
        print(f"  {key}. {label}")

    suffix = f" [{default}]" if default else ""
    while True:
        raw = input(f"请选择{suffix}: ").strip()
        if not raw and default:
            return default
        for key, _ in options:
            if raw == key:
                return key
        print("  输入无效，请重新选择。")


def build_menu_args(args: argparse.Namespace) -> argparse.Namespace:
    print("=" * 48)
    print(" ProductList 部署菜单")
    print("=" * 48)

    action = prompt_choice(
        "请选择操作",
        [
            ("1", "首次部署（构建镜像 + 上传全部制品 + 启动容器）"),
            ("2", "日常更新（上传 jar + dist 并重启容器）"),
            ("3", "更新版本号文件 (VERSION.md)"),
            ("4", "更新附件 (uploads)"),
            ("5", "更新文档 (docs)"),
            ("6", "更新所有数据 (uploads/docs)"),
            ("7", "退出"),
        ],
        default="2",
    )

    if action == "7":
        raise SystemExit(0)

    if action == "1":
        args.init = True
        args.update_data = False
    elif action == "2":
        args.init = False
        args.update_data = False
    elif action == "3":
        args.init = False
        args.update_version = True
        args.update_data = False
    elif action == "4":
        args.init = False
        args.update_data = True
        args.data_items = "uploads"
    elif action == "5":
        args.init = False
        args.update_data = True
        args.data_items = "docs"
    elif action == "6":
        args.init = False
        args.update_data = True
        args.data_items = "all"

    skip_build = prompt_choice(
        "是否跳过本地编译",
        [
            ("1", "否，执行本地编译"),
            ("2", "是，使用已有 jar / dist"),
        ],
        default="1",
    )
    args.skip_build = skip_build == "2"

    backend_jar = input("后端 jar 路径（直接回车自动查找）: ").strip()
    if backend_jar:
        args.backend_jar = backend_jar

    print()
    print("当前选择:")
    print(f"  host         : {args.host}")
    print(f"  remote-dir   : {args.remote_dir}")
    print(f"  init         : {args.init}")
    print(f"  update-data  : {args.update_data}")
    print(f"  data-items   : {args.data_items}")
    print(f"  skip-build   : {args.skip_build}")
    print(f"  backend-jar  : {args.backend_jar or '(自动)'}")

    confirm = prompt_choice(
        "确认执行",
        [
            ("1", "开始执行"),
            ("2", "退出"),
        ],
        default="1",
    )
    if confirm == "2":
        raise SystemExit(0)

    return args


# ======================= 主入口 =======================
def main():
    args = parse_args()
    repo_root = Path(__file__).parent.resolve()

    # 初始化属性，防止 getattr 报错
    args.update_version = getattr(args, "update_version", False)

    if args.menu:
        args = build_menu_args(args)
    elif args.version or args.uploads or args.docs:
        if args.version:
            args.update_version = True
        else:
            args.update_data = True

    if args.init:
        do_init(args, repo_root)
    elif args.update_version:
        do_update_version(args, repo_root)
    elif args.update_data:
        do_update_data(args, repo_root)
    else:
        do_deploy(args, repo_root)


if __name__ == "__main__":
    sys.exit(main())
