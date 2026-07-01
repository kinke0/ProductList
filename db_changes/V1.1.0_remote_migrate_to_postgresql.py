#!/usr/bin/env python3
"""
远程 SQLite → PostgreSQL 数据迁移脚本（本地生成SQL + 远程执行）
================================================================
在本地读取 SQLite 数据，生成带类型转换的 INSERT SQL 文件，
然后通过 SSH 上传到远程服务器，在远程 PG 容器中执行。

关键处理：
- SQLite 的毫秒级时间戳整数 → PostgreSQL 的 timestamp 字符串
- SQLite 的 0/1 → PostgreSQL boolean 的 true/false
- ON_ERROR_STOP=1 确保遇错停止

用法：
  python3 db_changes/V1.1.0_remote_migrate_to_postgresql.py
"""

import sqlite3
import sys
import os
import paramiko
import time
from datetime import datetime

# 远程服务器配置
HOST = os.environ.get("REMOTE_HOST", "10.104.6.53")
USER = os.environ.get("REMOTE_USER", "root")
PASSWORD = os.environ.get("REMOTE_PASSWORD", "his.123456")

# PostgreSQL 容器名
PG_CONTAINER = "productlist-pg"

# 本地 SQLite 文件路径（使用从服务器下载的 SQLite，确保数据一致）
SQLITE_DB = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "server_superpower.db")

# 所有需要迁移的表（按依赖顺序）
TABLES = [
    "base_category", "base_domain", "base_product",
    "base_product_l1", "base_product_l2",
    "sys_option",
    "sys_role", "sys_menu", "sys_user",
    "data_version", "data_entry",
    "custom_tab", "custom_tab_entry",
    "image_resource", "doc_gen_record",
    "operation_log", "approval_log",
    "req_item", "req_log",
]


def escape_sql_value(val, pg_type=None):
    """将 Python 值转为 SQL 值，处理 timestamp 和 boolean 类型"""
    if val is None:
        return "NULL"

    # PG timestamp 列：毫秒时间戳 → 字符串格式
    if pg_type and 'timestamp' in pg_type.lower():
        if isinstance(val, (int, float)):
            ts_seconds = val / 1000.0
            try:
                dt = datetime.fromtimestamp(ts_seconds)
                return f"'{dt.strftime('%Y-%m-%d %H:%M:%S')}'"
            except (OSError, ValueError, OverflowError):
                return "NULL"
        elif isinstance(val, str):
            return f"'{val.replace("'", "''")}'"

    # PG boolean 列：0/1 → true/false
    if pg_type and pg_type.lower() == 'boolean':
        if isinstance(val, int):
            return 'true' if val == 1 else 'false'
        elif isinstance(val, str):
            if val in ('1', 'true', 'TRUE', 'True'):
                return 'true'
            elif val in ('0', 'false', 'FALSE', 'False', ''):
                return 'false'
            else:
                return 'NULL'
        elif val is None:
            return 'NULL'
        else:
            return 'true' if val else 'false'

    if isinstance(val, (int, float)):
        if isinstance(val, float) and val != val:  # NaN
            return "NULL"
        return str(val)
    elif isinstance(val, str):
        escaped = val.replace("'", "''")
        return f"'{escaped}'"
    elif isinstance(val, bytes):
        hex_str = val.hex()
        return f"'\\x{hex_str}'"
    else:
        return f"'{str(val)}'"


def get_sqlite_columns(cursor, table):
    cursor.execute(f"PRAGMA table_info({table})")
    return [row[1] for row in cursor.fetchall()]


def get_pg_column_types_via_ssh(ssh, table):
    """通过远程 docker exec 获取 PG 表列名和类型"""
    sql = (
        f"SELECT column_name, data_type FROM information_schema.columns "
        f"WHERE table_name = '{table}' AND table_schema = 'public' "
        f"ORDER BY ordinal_position"
    )
    cmd = f'docker exec {PG_CONTAINER} psql -U productlist -d productlist -t -A -c "{sql}"'
    _, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode().strip()
    cols = {}
    for line in out.split("\n"):
        line = line.strip()
        if line and "|" in line:
            parts = line.split("|")
            if len(parts) == 2:
                cols[parts[0]] = parts[1]
    return cols


def generate_insert_sql(sqlite_cur, table, common_cols, pg_types):
    cols_str = ", ".join(common_cols)
    sqlite_cur.execute(f"SELECT {cols_str} FROM {table}")
    rows = sqlite_cur.fetchall()

    statements = []
    for row in rows:
        values = []
        for i, col in enumerate(common_cols):
            val = row[i]
            pg_type = pg_types.get(col)
            values.append(escape_sql_value(val, pg_type))
        values_str = ", ".join(values)
        statements.append(f"INSERT INTO {table} ({cols_str}) VALUES ({values_str});")

    return statements, len(rows)


def execute_sql_on_remote_pg(ssh, sql_content, table_name="migrate", batch_idx=0):
    """通过远程 docker exec 在 PG 容器内执行 SQL，使用唯一文件名避免冲突"""
    # 每个表每个批次使用唯一文件名，避免并发覆盖
    unique_name = f"migrate_{table_name}_{batch_idx}.sql"
    remote_sql_path = f"/tmp/{unique_name}"
    container_sql_path = f"/tmp/{unique_name}"

    # 1. 写入远程文件
    sftp = ssh.open_sftp()
    with sftp.open(remote_sql_path, 'w') as f:
        f.write(sql_content)
    sftp.close()

    # 2. 复制到 PG 容器（同步等待完成）
    cmd = f"docker cp {remote_sql_path} {PG_CONTAINER}:{container_sql_path}"
    _, stdout, stderr = ssh.exec_command(cmd)
    code = stdout.channel.recv_exit_status()
    if code != 0:
        err = stderr.read().decode()
        print(f"  [WARN] docker cp 失败: {err[:200]}")
        return code, "", err

    # 3. 验证文件已在容器内
    _, stdout, _ = ssh.exec_command(f"docker exec {PG_CONTAINER} test -f {container_sql_path} && echo OK")
    check = stdout.read().decode().strip()
    if check != "OK":
        print(f"  [WARN] 容器内文件不存在，等待...")
        time.sleep(2)

    # 4. 执行 psql（同步等待）
    cmd = f"docker exec {PG_CONTAINER} psql -U productlist -d productlist -v ON_ERROR_STOP=1 -f {container_sql_path}"
    _, stdout, stderr = ssh.exec_command(cmd)
    code = stdout.channel.recv_exit_status()
    out = stdout.read().decode()
    err = stderr.read().decode()

    # 5. 清理
    ssh.exec_command(f"docker exec {PG_CONTAINER} rm -f {container_sql_path}")
    ssh.exec_command(f"rm -f {remote_sql_path}")

    return code, out, err


def truncate_all_tables(ssh):
    all_tables = ", ".join(TABLES)
    sql = f"TRUNCATE TABLE {all_tables} CASCADE;"
    cmd = f'docker exec {PG_CONTAINER} psql -U productlist -d productlist -c "{sql}"'
    _, stdout, stderr = ssh.exec_command(cmd)
    code = stdout.channel.recv_exit_status()
    if code == 0:
        print("  [OK] 所有 PG 表已清空")
    else:
        err = stderr.read().decode()
        print(f"  [WARN] 可能有问题: {err[:200]}")


def reset_sequences(ssh):
    sql = """
    DO $$
    DECLARE
        r RECORD;
        max_id BIGINT;
    BEGIN
        FOR r IN
            SELECT c.table_name, c.column_name
            FROM information_schema.columns c
            JOIN information_schema.tables t ON c.table_name = t.table_name
            WHERE c.table_schema = 'public'
              AND c.column_name = 'id'
              AND t.table_type = 'BASE TABLE'
              AND t.table_schema = 'public'
        LOOP
            EXECUTE format('SELECT COALESCE(MAX(id), 0) FROM %I', r.table_name) INTO max_id;
            IF max_id > 0 THEN
                EXECUTE format('SELECT setval(pg_get_serial_sequence(%L, %L), %s)',
                    r.table_name, r.column_name, max_id);
            END IF;
        END LOOP;
    END $$;
    """
    remote_path = "/tmp/reset_sequences.sql"
    container_path = "/tmp/reset_sequences.sql"

    sftp = ssh.open_sftp()
    with sftp.open(remote_path, 'w') as f:
        f.write(sql)
    sftp.close()

    # 同步等待 docker cp 完成
    _, stdout, stderr = ssh.exec_command(f"docker cp {remote_path} {PG_CONTAINER}:{container_path}")
    stdout.channel.recv_exit_status()

    # 同步执行 psql
    cmd = f'docker exec {PG_CONTAINER} psql -U productlist -d productlist -f {container_path}'
    _, stdout, stderr = ssh.exec_command(cmd)
    code = stdout.channel.recv_exit_status()
    out = stdout.read().decode()
    err = stderr.read().decode()

    ssh.exec_command(f"docker exec {PG_CONTAINER} rm -f {container_path}")
    ssh.exec_command(f"rm -f {remote_path}")

    if code == 0 or "DO" in out:
        print("  [OK] 序列重置完成")
    else:
        print(f"  [WARN] 序列重置可能有问题: {err[:200]}")


def verify_table_count(ssh, table):
    cmd = f'docker exec {PG_CONTAINER} psql -U productlist -d productlist -t -A -c "SELECT count(*) FROM {table}"'
    _, stdout, _ = ssh.exec_command(cmd)
    out = stdout.read().decode().strip()
    try:
        return int(out)
    except ValueError:
        return 0


def migrate():
    print("=" * 50)
    print(" 远程 SQLite → PostgreSQL 数据迁移")
    print("=" * 50)

    # 连接远程服务器
    print(f"\n连接服务器 {HOST}...")
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, 22, USER, PASSWORD)
    print("  [OK] SSH 连接成功")

    # 检查 PG 容器
    _, stdout, _ = ssh.exec_command(f"docker ps --filter name={PG_CONTAINER} --format '{{.Status}}'")
    pg_status = stdout.read().decode().strip()
    if not pg_status:
        print(f"[ERROR] PG 容器 {PG_CONTAINER} 未运行")
        sys.exit(1)
    print(f"  PG 容器状态: {pg_status}")

    # 连接本地 SQLite
    if not os.path.exists(SQLITE_DB):
        print(f"[ERROR] SQLite 文件不存在: {SQLITE_DB}")
        sys.exit(1)
    print(f"  本地 SQLite: {SQLITE_DB}")
    sqlite_conn = sqlite3.connect(SQLITE_DB)
    sqlite_cur = sqlite_conn.cursor()

    # 1. 清空 PG 表
    print("\n[1] 清空 PG 表数据...")
    truncate_all_tables(ssh)

    # 2. 逐表迁移
    total = 0
    failed_tables = []

    for table in TABLES:
        print(f"\n迁移表: {table}")

        # 获取 PG 列名和类型（通过远程）
        pg_types = get_pg_column_types_via_ssh(ssh, table)
        pg_cols = list(pg_types.keys())
        if not pg_cols:
            print(f"  [SKIP] 表 {table} 在 PG 中不存在")
            continue

        # 获取 SQLite 列（本地）
        sqlite_cols = get_sqlite_columns(sqlite_cur, table)
        if not sqlite_cols:
            print(f"  [SKIP] 表 {table} 在 SQLite 中不存在")
            continue

        # 取交集列
        common_cols = [c for c in sqlite_cols if c in pg_cols]
        if not common_cols:
            print(f"  [SKIP] 表 {table} 无公共列")
            continue

        print(f"  公共列: {len(common_cols)} 个")

        # 本地生成 INSERT SQL
        statements, row_count = generate_insert_sql(sqlite_cur, table, common_cols, pg_types)
        if not statements:
            print(f"  [EMPTY] 表 {table} 无数据")
            continue

        print(f"  生成 {len(statements)} 条 INSERT，期望 {row_count} 行")

        # 分批上传执行（每批 2000 条，避免 SQL 过大）
        batch_size = 2000
        table_failed = False
        for batch_start in range(0, len(statements), batch_size):
            batch_idx = batch_start // batch_size
            batch = statements[batch_start:batch_start + batch_size]
            all_sql = "\n".join(batch)
            code, out, err = execute_sql_on_remote_pg(ssh, all_sql, table_name=table, batch_idx=batch_idx)
            if code != 0:
                error_msg = err[:500] if err else out[:500]
                print(f"  [FAIL] 批次 {batch_idx + 1} 失败: {error_msg}")
                table_failed = True
                break

        if table_failed:
            failed_tables.append(table)

        if table not in failed_tables:
            actual_count = verify_table_count(ssh, table)
            print(f"  [OK] 期望 {row_count} 行，实际 {actual_count} 行")
            total += actual_count

    # 3. 重置序列
    print("\n[3] 重置 PostgreSQL 序列...")
    reset_sequences(ssh)

    # 关闭
    sqlite_cur.close()
    sqlite_conn.close()
    ssh.close()

    print(f"\n{'=' * 50}")
    print(f"迁移完成！共迁移 {total} 行数据")
    if failed_tables:
        print(f"失败表: {failed_tables}")
    print(f"{'=' * 50}")


if __name__ == "__main__":
    migrate()
