#!/usr/bin/env python3
"""
SQLite → PostgreSQL 数据迁移脚本（无需 psycopg2）
==================================================
使用标准库 sqlite3 读取数据，生成 SQL INSERT 语句，
通过 docker exec 在 PostgreSQL 容器内执行。

关键处理：
- SQLite 的毫秒级时间戳整数 → PostgreSQL 的 timestamp 字符串
- psql 执行时加 ON_ERROR_STOP=1，确保遇到错误就停止
- 中文列名自动处理

用法：
  python3 db_changes/V1.1.0_migrate_sqlite_to_postgresql.py

前提：
1. PostgreSQL 容器 productlist-pg 已运行
2. Spring Boot 已运行过一次（ddl-auto:update 自动建表），然后后端已停止
"""

import sqlite3
import sys
import os
import subprocess
import tempfile
from datetime import datetime

# SQLite 源文件
SQLITE_DB = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "superpower.db")

# PostgreSQL 容器名
PG_CONTAINER = os.environ.get("PG_CONTAINER", "productlist-pg")

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
    """将 Python 值转为 SQL 值，处理 timestamp 类型"""
    if val is None:
        return "NULL"

    # 如果 PG 列类型是 timestamp，且值是整数（毫秒时间戳），转换
    if pg_type and 'timestamp' in pg_type.lower():
        if isinstance(val, (int, float)):
            # 毫秒级时间戳 → PostgreSQL timestamp
            ts_seconds = val / 1000.0
            try:
                dt = datetime.fromtimestamp(ts_seconds)
                return f"'{dt.strftime('%Y-%m-%d %H:%M:%S')}'"
            except (OSError, ValueError, OverflowError):
                return "NULL"
        elif isinstance(val, str):
            # 可能已经是日期字符串格式
            return f"'{val.replace("'", "''")}'"

    # 如果 PG 列类型是 boolean，且值是整数 0/1，转换
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
    """获取 SQLite 表的列名列表"""
    cursor.execute(f"PRAGMA table_info({table})")
    return [row[1] for row in cursor.fetchall()]


def get_pg_column_types_via_docker(table):
    """通过 docker exec 获取 PostgreSQL 表的列名和类型"""
    sql = (
        f"SELECT column_name, data_type FROM information_schema.columns "
        f"WHERE table_name = '{table}' AND table_schema = 'public' ORDER BY ordinal_position"
    )
    result = subprocess.run(
        ["docker", "exec", PG_CONTAINER,
         "psql", "-U", "productlist", "-d", "productlist", "-t", "-A", "-c", sql],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        return {}
    cols = {}
    for line in result.stdout.strip().split("\n"):
        if line.strip():
            parts = line.strip().split("|")
            if len(parts) == 2:
                cols[parts[0]] = parts[1]
    return cols


def generate_insert_sql(sqlite_cur, table, common_cols, pg_types):
    """为单个表生成 INSERT SQL 语句"""
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


def execute_sql_via_docker(sql_content):
    """通过 docker exec 在 PostgreSQL 容器内执行 SQL，遇错停止"""
    with tempfile.NamedTemporaryFile(mode='w', suffix='.sql', delete=False, encoding='utf-8') as f:
        f.write(sql_content)
        temp_path = f.name

    # 将 SQL 文件复制到容器内
    container_path = "/tmp/migrate.sql"
    subprocess.run(
        ["docker", "cp", temp_path, f"{PG_CONTAINER}:{container_path}"],
        check=True
    )

    # 在容器内执行（加 ON_ERROR_STOP=1）
    result = subprocess.run(
        ["docker", "exec", PG_CONTAINER,
         "psql", "-U", "productlist", "-d", "productlist",
         "-v", "ON_ERROR_STOP=1", "-f", container_path],
        capture_output=True, text=True
    )

    # 清理
    os.unlink(temp_path)
    subprocess.run(
        ["docker", "exec", PG_CONTAINER, "rm", "-f", container_path],
        capture_output=True
    )

    return result


def reset_sequences_via_docker():
    """重置 PostgreSQL 序列"""
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
    result = subprocess.run(
        ["docker", "exec", PG_CONTAINER,
         "psql", "-U", "productlist", "-d", "productlist", "-c", sql],
        capture_output=True, text=True
    )
    if result.returncode == 0:
        print("  [OK] 序列重置完成")
    else:
        print(f"  [WARN] 序列重置可能有问题: {result.stderr}")


def migrate():
    """执行完整迁移"""
    print("=" * 50)
    print(" SQLite → PostgreSQL 数据迁移")
    print("=" * 50)

    # 检查 PostgreSQL 容器是否运行
    result = subprocess.run(
        ["docker", "ps", "--filter", f"name={PG_CONTAINER}", "--format", "{{.Status}}"],
        capture_output=True, text=True
    )
    if not result.stdout.strip():
        print(f"[ERROR] PostgreSQL 容器 {PG_CONTAINER} 未运行")
        sys.exit(1)
    print(f"PostgreSQL 容器状态: {result.stdout.strip()}")

    # 连接 SQLite
    if not os.path.exists(SQLITE_DB):
        print(f"[ERROR] SQLite 文件不存在: {SQLITE_DB}")
        sys.exit(1)

    print(f"读取 SQLite: {SQLITE_DB}")
    sqlite_conn = sqlite3.connect(SQLITE_DB)
    sqlite_cur = sqlite_conn.cursor()

    total = 0
    failed_tables = []
    for table in TABLES:
        print(f"\n迁移表: {table}")

        # 获取 SQLite 列
        sqlite_cols = get_sqlite_columns(sqlite_cur, table)
        if not sqlite_cols:
            print(f"  [SKIP] 表 {table} 在 SQLite 中不存在")
            continue

        # 获取 PostgreSQL 列名和类型
        pg_types = get_pg_column_types_via_docker(table)
        pg_cols = list(pg_types.keys())
        if not pg_cols:
            print(f"  [SKIP] 表 {table} 在 PostgreSQL 中不存在（需先启动 Spring Boot 建表）")
            continue

        # 取交集列
        common_cols = [c for c in sqlite_cols if c in pg_cols]
        if not common_cols:
            print(f"  [SKIP] 表 {table} 无公共列")
            continue

        # 生成 INSERT SQL（带类型转换）
        statements, row_count = generate_insert_sql(sqlite_cur, table, common_cols, pg_types)
        if not statements:
            print(f"  [EMPTY] 表 {table} 无数据")
            continue

        # 先清空目标表
        delete_sql = f"DELETE FROM {table};\n"
        all_sql = delete_sql + "\n".join(statements)

        # 执行
        result = execute_sql_via_docker(all_sql)
        if result.returncode == 0:
            # 验证实际行数
            verify_result = subprocess.run(
                ["docker", "exec", PG_CONTAINER,
                 "psql", "-U", "productlist", "-d", "productlist", "-t", "-A",
                 "-c", f"SELECT count(*) FROM {table}"],
                capture_output=True, text=True
            )
            actual_count = int(verify_result.stdout.strip()) if verify_result.stdout.strip() else 0
            print(f"  [OK] 表 {table}: 期望 {row_count} 行，实际 {actual_count} 行")
            total += actual_count
        else:
            print(f"  [FAIL] 表 {table} 迁移失败:")
            print(f"    错误: {result.stderr[:300]}")
            failed_tables.append(table)

    # 重置序列
    print("\n重置 PostgreSQL 序列...")
    reset_sequences_via_docker()

    # 关闭 SQLite 连接
    sqlite_cur.close()
    sqlite_conn.close()

    print(f"\n{'=' * 50}")
    print(f"迁移完成！共迁移 {total} 行数据")
    if failed_tables:
        print(f"失败表: {failed_tables}")
    print(f"{'=' * 50}")


if __name__ == "__main__":
    migrate()
