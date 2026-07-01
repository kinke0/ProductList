# V1.1.0 数据库迁移：SQLite → PostgreSQL

## Context

当前系统使用 SQLite，已出现并发写锁冲突（SQLITE_BUSY_SNAPSHOT），且 SQLite 不支持并发写入、缺乏递归查询原生支持。迁移到 PostgreSQL 可解决这些问题，并为未来功能扩展提供更好的基础。

## CodeGraph 分析结果

### 需要修改的文件
- `pom.xml` — 替换 sqlite-jdbc 依赖为 postgresql 驱动
- `application.yml` — 替换数据库连接配置
- `SqliteConfig.java` — 删除或替换为 PgConfig（PostgreSQL 不需要 PRAGMA 设置）
- `SqliteDialect.java` — 删除，改为使用 Hibernate 官方 PostgreSQL 方言
- `Dockerfile` — 不需要修改（PostgreSQL 作为独立容器运行）
- `docker/entrypoint.sh` — 不需要修改
- `deploy.py` — 修改数据库备份和上传逻辑

### 需要注意的问题
1. **中文列名**：46个 `@Column(name = "col_xxx")` 使用中文列名，PostgreSQL 支持中文列名但需要双引号引用。Hibernate `ddl-auto: update` 会自动处理。
2. **columnDefinition = "TEXT"**：11处使用 SQLite 的 TEXT 类型定义，PostgreSQL 也支持 TEXT 类型，无需修改。
3. **JPA 查询**：所有 `@Query` 都是 JPQL（非 nativeQuery），不依赖特定数据库方言，无需修改。
4. **IDENTITY 主键生成**：`@GeneratedValue(strategy = GenerationType.IDENTITY)` 在 PostgreSQL 中使用 SERIAL/BIGSERIAL，与 SQLite 的 AUTOINCREMENT 行为一致。
5. **HikariCP 连接池**：当前 `maximum-pool-size: 1` 是 SQLite 的权宜之计，迁移后可恢复为 5-10。
6. **数据迁移**：需要将现有 SQLite 数据导出并导入 PostgreSQL。

## 实施步骤

### Step 1: 修改 pom.xml 依赖

替换 sqlite-jdbc 为 postgresql 驱动：

```xml
<!-- 删除 -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.1.0</version>
</dependency>

<!-- 新增 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
</dependency>
```

### Step 2: 修改 application.yml

替换数据库连接配置，恢复连接池大小：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/productlist
    username: productlist
    password: productlist123
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 5
      connection-timeout: 60000
      leak-detection-threshold: 30000
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
```

### Step 3: 删除 SqliteConfig.java 和 SqliteDialect.java

- `SqliteConfig.java` — PostgreSQL 不需要 PRAGMA 设置，删除此配置类
- `SqliteDialect.java` — 使用 Hibernate 官方 PostgreSQLDialect，删除此方言类

### Step 4: 创建 application-prod.yml

为生产环境创建单独配置文件，连接远程 PostgreSQL：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://productlist-pg:5432/productlist
    username: productlist
    password: ${PG_PASSWORD}
```

### Step 5: 修改 Docker 部署配置

创建 `docker-compose.yml`，添加 PostgreSQL 容器：

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: productlist
      POSTGRES_USER: productlist
      POSTGRES_PASSWORD: productlist123
    volumes:
      - pgdata:/var/lib/postgresql/data
    ports:
      - "5432:5432"
  
  app:
    build: .
    depends_on:
      - postgres
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/productlist
    volumes:
      - ./data/dist:/usr/share/nginx/html
      - ./data/uploads:/app/uploads
      - ./data/docs:/app/generated-docs
```

### Step 6: 修改 deploy.py

- 移除 SQLite 数据库文件备份和上传逻辑
- 改为 PostgreSQL 数据库备份（pg_dump）
- 修改 Docker 容器启动命令（添加 PostgreSQL 容器）

### Step 7: 数据迁移脚本

编写 Python 脚本将 SQLite 数据导出并导入 PostgreSQL：
- 读取 SQLite 所有表数据
- 写入 PostgreSQL 对应表
- 处理中文列名的双引号引用

### Step 8: 更新 VERSION.md

### Step 9: 验证

1. 本地安装 PostgreSQL（Docker 或直接安装）
2. 后端编译确认无错误
3. 前端构建确认无错误
4. 启动服务验证所有功能正常
5. 数据迁移验证：对比迁移前后数据一致性
