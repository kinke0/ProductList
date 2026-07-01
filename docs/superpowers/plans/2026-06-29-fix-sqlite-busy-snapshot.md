# 修复版本删除 SQLITE_BUSY_SNAPSHOT 错误

## Context

用户反馈：服务器上删除版本时报错 `SQLITE_BUSY_SNAPSHOT: Another database connection has already written to the database (database is locked)`。

这不是数据库表字段未更新的问题，而是 SQLite WAL 模式下的并发写锁冲突。

## CodeGraph 分析结果

- `deleteVersion` (DataVersionService.java:506) — 版本删除入口，先在主线程执行一个读事务获取版本号，再在异步线程执行9步删除大事务
- `SqliteConfig` (SqliteConfig.java:12) — 设置 `PRAGMA journal_mode=WAL` 和 `busy_timeout=30000`
- `application.yml` — HikariCP `maximum-pool-size: 3`，SQLite 单写模式下多连接导致 WAL snapshot 冲突

## 根因分析

SQLite 在 WAL 模式下同一时刻只允许一个写事务。HikariCP 连接池大小为 3，意味着最多 3 个数据库连接。当两个不同连接分别持有 WAL snapshot 并尝试写入时，第二个连接会发现数据库已被第一个修改，触发 `SQLITE_BUSY_SNAPSHOT`。

`deleteVersion` 方法先在主线程执行一个读事务（第508行），再在异步线程执行大删除事务（第533行），两个事务可能使用不同的 HikariCP 连接。如果有其他请求在两个事务之间触发了数据库写入，就会导致 WAL snapshot 冲突。

## 修改文件

- `src/main/resources/application.yml`

## 实施步骤

### Step 1: 将 HikariCP maximum-pool-size 改为 1

SQLite 本质上不支持并发写入，将连接池限制为 1 可确保所有操作通过同一连接完成，彻底避免 WAL snapshot 冲突：

```yaml
# 修改前
hikari:
  maximum-pool-size: 3

# 修改后
hikari:
  maximum-pool-size: 1
```

### Step 2: 更新 VERSION.md

### Step 3: 验证
1. 后端编译确认无错误
2. 前端构建确认无错误
3. 重启前后端服务，curl验证端口正常
4. curl调用关键接口检查无500错误
