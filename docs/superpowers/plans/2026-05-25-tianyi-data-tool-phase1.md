# 添翼产品清单管理工具 — 实现计划（Phase 1：后端基础 + 数据维护）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 搭建 Spring Boot 后端基础框架，实现用户认证、权限管理和数据条目（data_entry）的树形 CRUD 功能。

**架构：** Spring Boot 单体应用，JWT 认证，SQLite 数据库（保留 PostgreSQL 迁移脚本），RESTful API 接口。

**Tech Stack:** Spring Boot 3.x, Spring Security, JWT, Spring Data JPA, SQLite (开发期), PostgreSQL (生产)

---

### 项目文件结构

```
superpower-test/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/superpower/
│   │   │   ├── SuperPowerApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── WebMvcConfig.java
│   │   │   │   └── SqliteDialect.java
│   │   │   ├── common/
│   │   │   │   ├── Result.java
│   │   │   │   ├── ResultCode.java
│   │   │   │   ├── PageResult.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── BusinessException.java
│   │   │   ├── security/
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── CustomUserDetailsService.java
│   │   │   ├── modules/
│   │   │   │   ├── system/
│   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── SysUser.java
│   │   │   │   │   │   ├── SysRole.java
│   │   │   │   │   │   └── SysMenu.java
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── SysUserRepository.java
│   │   │   │   │   │   ├── SysRoleRepository.java
│   │   │   │   │   │   └── SysMenuRepository.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── SysUserService.java
│   │   │   │   │   │   ├── SysRoleService.java
│   │   │   │   │   │   └── SysMenuService.java
│   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── AuthController.java
│   │   │   │   │   │   ├── SysUserController.java
│   │   │   │   │   │   └── SysRoleController.java
│   │   │   │   │   └── dto/
│   │   │   │   │       ├── LoginRequest.java
│   │   │   │   │       ├── LoginResponse.java
│   │   │   │   │       └── UserDTO.java
│   │   │   │   └── data/
│   │   │   │       ├── entity/
│   │   │   │       │   └── DataEntry.java
│   │   │   │       ├── repository/
│   │   │   │       │   └── DataEntryRepository.java
│   │   │   │       ├── service/
│   │   │   │       │   └── DataEntryService.java
│   │   │   │       ├── controller/
│   │   │   │       │   └── DataEntryController.java
│   │   │   │       └── dto/
│   │   │   │           ├── DataEntryDTO.java
│   │   │   │           └── TreeNodeDTO.java
│   │   │   └── version/
│   │   │       ├── entity/
│   │   │       │   └── DataVersion.java
│   │   │       ├── repository/
│   │   │       │   └── DataVersionRepository.java
│   │   │       ├── service/
│   │   │       │   └── DataVersionService.java
│   │   │       └── controller/
│   │   │           └── DataVersionController.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/
│   │           └── init.sql
│   └── test/
│       └── java/com/superpower/
│           ├── SuperPowerApplicationTests.java
│           ├── modules/data/
│           │   └── DataEntryServiceTest.java
│           └── modules/system/
│               ├── AuthControllerTest.java
│               └── SysUserServiceTest.java
```

---

### Task 1: 初始化 Spring Boot 项目 + SQLite 配置

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/superpower/SuperPowerApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/db/init.sql`
- Create: `src/main/java/com/superpower/config/SqliteDialect.java`

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    <groupId>com.superpower</groupId>
    <artifactId>superpower-test</artifactId>
    <version>1.0.0</version>
    <name>superpower-test</name>

    <properties>
        <java.version>17</java.version>
        <jjwt.version>0.12.3</jjwt.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>3.45.1.0</version>
        </dependency>
        <dependency>
            <groupId>org.hibernate.orm</groupId>
            <artifactId>hibernate-community-dialects</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:sqlite:superpower.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    database-platform: com.superpower.config.SqliteDialect
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true

app:
  jwt:
    secret: superpower-test-secret-key-for-jwt-token-generation-2026
    expiration-ms: 86400000
```

- [ ] **Step 3: Create SqliteDialect.java**

```java
package com.superpower.config;

import org.hibernate.community.dialect.SQLiteDialect;

public class SqliteDialect extends SQLiteDialect {
    public SqliteDialect() {
        super();
    }
}
```

- [ ] **Step 4: Create SuperPowerApplication.java**

```java
package com.superpower;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SuperPowerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SuperPowerApplication.class, args);
    }
}
```

- [ ] **Step 5: Create init.sql** (PostgreSQL 迁移脚本占位)

```sql
-- PostgreSQL 迁移脚本
-- 执行 DDL 前请先创建数据库: CREATE DATABASE superpower;

-- sys_user
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    role_id BIGINT,
    status INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- sys_role
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- sys_role_menu
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    UNIQUE(role_id, menu_id)
);

-- sys_menu
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT,
    name VARCHAR(100) NOT NULL,
    permission VARCHAR(100),
    type INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- data_version
CREATE TABLE IF NOT EXISTS data_version (
    id BIGSERIAL PRIMARY KEY,
    version_no VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'draft',
    released_at TIMESTAMP,
    released_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- data_version_changelog
CREATE TABLE IF NOT EXISTS data_version_changelog (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL,
    entry_id BIGINT,
    change_type VARCHAR(20) NOT NULL,
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    operated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- data_entry
CREATE TABLE IF NOT EXISTS data_entry (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL,
    parent_id BIGINT,
    level INTEGER NOT NULL,
    sort_order INTEGER DEFAULT 0,
    is_leaf BOOLEAN DEFAULT TRUE,
    col_产品系统 VARCHAR(500),
    col_应用角色 VARCHAR(500),
    col_招标参数说明 TEXT,
    col_功能说明 TEXT,
    col_状态 VARCHAR(100),
    col_业务分类 VARCHAR(200),
    col_业务域 VARCHAR(200),
    col_版本划分 VARCHAR(200),
    col_远 VARCHAR(50),
    col_交付工作量人月 VARCHAR(100),
    col_控标点 VARCHAR(50),
    col_控标点截图1 TEXT,
    col_控标点截图2 TEXT,
    col_控标点截图3 TEXT,
    col_控标点文档说明 TEXT,
    col_软著 VARCHAR(500),
    col_备注 TEXT,
    col_智慧医疗 VARCHAR(100),
    col_智慧服务 VARCHAR(100),
    col_智慧管理 VARCHAR(100),
    col_互联互通 VARCHAR(100),
    col_产品系统标识 VARCHAR(100),
    col_模块标识 VARCHAR(100),
    col_其他解决方案标记 VARCHAR(200),
    col_文档维护人员 VARCHAR(100),
    col_产品经理 VARCHAR(100),
    col_父记录 VARCHAR(500),
    col_内部版本 VARCHAR(100),
    col_智能化 VARCHAR(50),
    col_曜 VARCHAR(50),
    col_驰 VARCHAR(50),
    col_FY23 NUMERIC,
    col_FY24 NUMERIC,
    col_FY25 NUMERIC,
    col_FY26 NUMERIC,
    col_FY27 NUMERIC,
    col_FY28 NUMERIC,
    col_FY29 NUMERIC,
    col_研发成本合计 NUMERIC,
    col_销量曜 INTEGER,
    col_销量远 INTEGER,
    col_销量驰 INTEGER,
    col_出厂套价保本 NUMERIC,
    col_负责人 VARCHAR(200),
    col_产品线 VARCHAR(200),
    col_资产类型 VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT
);
```

- [ ] **Step 6: 验证项目启动**

Run: `mvn spring-boot:run`
Expected: 应用启动无报错，SQLite 数据库 superpower.db 自动创建

---

### Task 2: 通用响应封装 + 全局异常处理

**Files:**
- Create: `src/main/java/com/superpower/common/ResultCode.java`
- Create: `src/main/java/com/superpower/common/Result.java`
- Create: `src/main/java/com/superpower/common/PageResult.java`
- Create: `src/main/java/com/superpower/common/BusinessException.java`
- Create: `src/main/java/com/superpower/common/GlobalExceptionHandler.java`

- [ ] **Step 1: Create ResultCode.java**

```java
package com.superpower.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAILED(400, "操作失败"),
    UNAUTHORIZED(401, "未登录或token已过期"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    VALIDATE_FAILED(422, "参数校验失败"),
    INTERNAL_ERROR(500, "系统内部错误");

    private final int code;
    private final String message;
}
```

- [ ] **Step 2: Create Result.java**

```java
package com.superpower.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> failed(String message) {
        return new Result<>(ResultCode.FAILED.getCode(), message, null);
    }

    public static <T> Result<T> failed(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> forbidden() {
        return new Result<>(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage(), null);
    }

    public static <T> Result<T> unauthorized() {
        return new Result<>(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage(), null);
    }
}
```

- [ ] **Step 3: Create PageResult.java**

```java
package com.superpower.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int page;
    private int pageSize;

    public PageResult(List<T> list, long total, int page, int pageSize) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }
}
```

- [ ] **Step 4: Create BusinessException.java**

```java
package com.superpower.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAILED.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

- [ ] **Step 5: Create GlobalExceptionHandler.java**

```java
package com.superpower.common;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return new Result<>(e.getCode(), e.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException() {
        return Result.forbidden();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return new Result<>(ResultCode.VALIDATE_FAILED.getCode(), msg, null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        return new Result<>(ResultCode.INTERNAL_ERROR.getCode(), e.getMessage(), null);
    }
}
```

---

### Task 3: JWT 认证 + Spring Security 配置

**Files:**
- Create: `src/main/java/com/superpower/security/JwtTokenProvider.java`
- Create: `src/main/java/com/superpower/security/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/superpower/security/CustomUserDetailsService.java`
- Create: `src/main/java/com/superpower/config/SecurityConfig.java`

- [ ] **Step 1: Create JwtTokenProvider.java**

```java
package com.superpower.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username, Long userId, String roleCode) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", roleCode)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: Create CustomUserDetailsService.java**

```java
package com.superpower.security;

import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.repository.SysUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserRepository userRepository;

    public CustomUserDetailsService(SysUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        String roleCode = sysUser.getRole() != null ? sysUser.getRole().getCode() : "ROLE_USER";

        return User.builder()
                .username(sysUser.getUsername())
                .password(sysUser.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + roleCode)))
                .disabled(sysUser.getStatus() != 1)
                .build();
    }
}
```

- [ ] **Step 3: Create JwtAuthenticationFilter.java**

```java
package com.superpower.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                    CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 4: Create SecurityConfig.java**

```java
package com.superpower.config;

import com.superpower.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                .requestMatchers("/api/**").authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

---

### Task 4: 系统用户 + 角色实体与 Repository

**Files:**
- Create: `src/main/java/com/superpower/modules/system/entity/SysUser.java`
- Create: `src/main/java/com/superpower/modules/system/entity/SysRole.java`
- Create: `src/main/java/com/superpower/modules/system/entity/SysMenu.java`
- Create: `src/main/java/com/superpower/modules/system/repository/SysUserRepository.java`
- Create: `src/main/java/com/superpower/modules/system/repository/SysRoleRepository.java`
- Create: `src/main/java/com/superpower/modules/system/repository/SysMenuRepository.java`

- [ ] **Step 1: Create SysRole.java**

```java
package com.superpower.modules.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_role")
public class SysRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **Step 2: Create SysMenu.java**

```java
package com.superpower.modules.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_menu")
public class SysMenu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String permission;

    @Column(nullable = false)
    private Integer type = 1;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **Step 3: Create SysUser.java**

```java
package com.superpower.modules.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_user")
public class SysUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 100)
    private String nickname;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private SysRole role;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
```

- [ ] **Step 4: Create SysUserRepository.java**

```java
package com.superpower.modules.system.repository;

import com.superpower.modules.system.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

- [ ] **Step 5: Create SysRoleRepository.java**

```java
package com.superpower.modules.system.repository;

import com.superpower.modules.system.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SysRoleRepository extends JpaRepository<SysRole, Long> {
    Optional<SysRole> findByCode(String code);
}
```

- [ ] **Step 6: Create SysMenuRepository.java**

```java
package com.superpower.modules.system.repository;

import com.superpower.modules.system.entity.SysMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SysMenuRepository extends JpaRepository<SysMenu, Long> {
    List<SysMenu> findByParentIdOrderBySortOrder(Long parentId);
}
```

---

### Task 5: 认证接口 + 用户服务

**Files:**
- Create: `src/main/java/com/superpower/modules/system/dto/LoginRequest.java`
- Create: `src/main/java/com/superpower/modules/system/dto/LoginResponse.java`
- Create: `src/main/java/com/superpower/modules/system/dto/UserDTO.java`
- Create: `src/main/java/com/superpower/modules/system/service/SysUserService.java`
- Create: `src/main/java/com/superpower/modules/system/service/SysRoleService.java`
- Create: `src/main/java/com/superpower/modules/system/service/SysMenuService.java`
- Create: `src/main/java/com/superpower/modules/system/controller/AuthController.java`
- Create: `src/main/java/com/superpower/modules/system/controller/SysUserController.java`
- Create: `src/main/java/com/superpower/modules/system/controller/SysRoleController.java`

- [ ] **Step 1: Create LoginRequest.java**

```java
package com.superpower.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

- [ ] **Step 2: Create LoginResponse.java**

```java
package com.superpower.modules.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String nickname;
    private String roleCode;
    private String roleName;
}
```

- [ ] **Step 3: Create UserDTO.java**

```java
package com.superpower.modules.system.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String nickname;
    private Long roleId;
    private String roleName;
    private String roleCode;
    private Integer status;
}
```

- [ ] **Step 4: Create SysUserService.java**

```java
package com.superpower.modules.system.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.system.dto.UserDTO;
import com.superpower.modules.system.entity.SysRole;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.repository.SysRoleRepository;
import com.superpower.modules.system.repository.SysUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysUserService {

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public SysUserService(SysUserRepository userRepository,
                          SysRoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public SysUser findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    public List<SysUser> findAll() {
        return userRepository.findAll();
    }

    public UserDTO createUser(String username, String password, String nickname, Long roleId) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }
        SysRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("角色不存在"));

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(1);
        user = userRepository.save(user);

        return toDTO(user);
    }

    public void updateUser(Long id, String nickname, Long roleId, Integer status) {
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (nickname != null) user.setNickname(nickname);
        if (roleId != null) {
            SysRole role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new BusinessException("角色不存在"));
            user.setRole(role);
        }
        if (status != null) user.setStatus(status);
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    private UserDTO toDTO(SysUser user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setRoleId(user.getRole().getId());
        dto.setRoleName(user.getRole().getName());
        dto.setRoleCode(user.getRole().getCode());
        dto.setStatus(user.getStatus());
        return dto;
    }
}
```

- [ ] **Step 5: Create SysRoleService.java**

```java
package com.superpower.modules.system.service;

import com.superpower.modules.system.entity.SysRole;
import com.superpower.modules.system.repository.SysRoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysRoleService {

    private final SysRoleRepository roleRepository;

    public SysRoleService(SysRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<SysRole> findAll() {
        return roleRepository.findAll();
    }

    public SysRole findByCode(String code) {
        return roleRepository.findByCode(code)
                .orElse(null);
    }

    public SysRole createRole(String name, String code, String description) {
        SysRole role = new SysRole();
        role.setName(name);
        role.setCode(code);
        role.setDescription(description);
        return roleRepository.save(role);
    }
}
```

- [ ] **Step 6: Create AuthController.java**

```java
package com.superpower.modules.system.controller;

import com.superpower.common.Result;
import com.superpower.modules.system.dto.LoginRequest;
import com.superpower.modules.system.dto.LoginResponse;
import com.superpower.modules.system.entity.SysRole;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.service.SysUserService;
import com.superpower.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserService userService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          SysUserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        SysUser user = userService.findByUsername(request.getUsername());
        SysRole role = user.getRole();
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId(), role.getCode());

        return Result.success(new LoginResponse(token, user.getUsername(), user.getNickname(),
                role.getCode(), role.getName()));
    }

    @GetMapping("/me")
    public Result<?> getCurrentUser(Authentication authentication) {
        SysUser user = userService.findByUsername(authentication.getName());
        return Result.success(user);
    }
}
```

- [ ] **Step 7: Create SysUserController.java**

```java
package com.superpower.modules.system.controller;

import com.superpower.common.Result;
import com.superpower.modules.system.dto.UserDTO;
import com.superpower.modules.system.service.SysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class SysUserController {

    private final SysUserService userService;

    public SysUserController(SysUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<List<UserDTO>> getAllUsers() {
        return Result.success(userService.findAll().stream()
                .map(user -> {
                    UserDTO dto = new UserDTO();
                    dto.setId(user.getId());
                    dto.setUsername(user.getUsername());
                    dto.setNickname(user.getNickname());
                    dto.setRoleId(user.getRole().getId());
                    dto.setRoleName(user.getRole().getName());
                    dto.setRoleCode(user.getRole().getCode());
                    dto.setStatus(user.getStatus());
                    return dto;
                }).toList());
    }

    @PostMapping
    public Result<UserDTO> createUser(@RequestBody UserDTO dto) {
        return Result.success(userService.createUser(
                dto.getUsername(), dto.getUsername(), dto.getNickname(), dto.getRoleId()));
    }

    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody UserDTO dto) {
        userService.updateUser(id, dto.getNickname(), dto.getRoleId(), dto.getStatus());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
```

- [ ] **Step 8: Create SysRoleController.java**

```java
package com.superpower.modules.system.controller;

import com.superpower.common.Result;
import com.superpower.modules.system.entity.SysRole;
import com.superpower.modules.system.service.SysRoleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class SysRoleController {

    private final SysRoleService roleService;

    public SysRoleController(SysRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public Result<List<SysRole>> getAllRoles() {
        return Result.success(roleService.findAll());
    }

    @PostMapping
    public Result<SysRole> createRole(@RequestBody SysRole role) {
        return Result.success(roleService.createRole(role.getName(), role.getCode(), role.getDescription()));
    }
}
```

---

### Task 6: 版本管理实体 + Repository + Service + Controller

**Files:**
- Create: `src/main/java/com/superpower/modules/version/entity/DataVersion.java`
- Create: `src/main/java/com/superpower/modules/version/repository/DataVersionRepository.java`
- Create: `src/main/java/com/superpower/modules/version/service/DataVersionService.java`
- Create: `src/main/java/com/superpower/modules/version/controller/DataVersionController.java`

- [ ] **Step 1: Create DataVersion.java**

```java
package com.superpower.modules.version.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_version")
public class DataVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_no", nullable = false, length = 20)
    private String versionNo;

    @Column(nullable = false, length = 20)
    private String status = "draft";

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "released_by")
    private Long releasedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
```

- [ ] **Step 2: Create DataVersionRepository.java**

```java
package com.superpower.modules.version.repository;

import com.superpower.modules.version.entity.DataVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DataVersionRepository extends JpaRepository<DataVersion, Long> {
    Optional<DataVersion> findTopByOrderByCreatedAtDesc();

    @Query("SELECT v FROM DataVersion v WHERE v.status = 'released' ORDER BY v.createdAt DESC")
    java.util.List<DataVersion> findAllReleased();
}
```

- [ ] **Step 3: Create DataVersionService.java**

```java
package com.superpower.modules.version.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DataVersionService {

    private final DataVersionRepository versionRepository;
    private final DataEntryRepository entryRepository;

    public DataVersionService(DataVersionRepository versionRepository,
                              DataEntryRepository entryRepository) {
        this.versionRepository = versionRepository;
        this.entryRepository = entryRepository;
    }

    public List<DataVersion> findAllReleased() {
        return versionRepository.findAllReleased();
    }

    public List<DataVersion> findAll() {
        return versionRepository.findAll();
    }

    public DataVersion findById(Long id) {
        return versionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("版本不存在"));
    }

    @Transactional
    public DataVersion createVersion() {
        String newVersionNo = "1.0";
        DataVersion latest = versionRepository.findTopByOrderByCreatedAtDesc().orElse(null);
        if (latest != null) {
            String lastNo = latest.getVersionNo();
            String[] parts = lastNo.split("\\.");
            int minor = Integer.parseInt(parts[1]) + 1;
            newVersionNo = parts[0] + "." + minor;
        }

        DataVersion version = new DataVersion();
        version.setVersionNo(newVersionNo);
        version.setStatus("draft");
        version = versionRepository.save(version);

        // 复制上一版本数据
        if (latest != null && "released".equals(latest.getStatus())) {
            List<DataEntry> entries = entryRepository.findByVersionId(latest.getId());
            for (DataEntry entry : entries) {
                DataEntry copy = entry.cloneWithoutId();
                copy.setVersionId(version.getId());
                entryRepository.save(copy);
            }
        }

        return version;
    }

    @Transactional
    public DataVersion releaseVersion(Long versionId, Long userId) {
        DataVersion version = findById(versionId);
        if (!"draft".equals(version.getStatus())) {
            throw new BusinessException("版本状态不正确");
        }
        version.setStatus("released");
        version.setReleasedAt(LocalDateTime.now());
        version.setReleasedBy(userId);
        return versionRepository.save(version);
    }
}
```

- [ ] **Step 4: Create DataVersionController.java**

```java
package com.superpower.modules.version.controller;

import com.superpower.common.Result;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.service.DataVersionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/versions")
public class DataVersionController {

    private final DataVersionService versionService;

    public DataVersionController(DataVersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping
    public Result<List<DataVersion>> getAllVersions() {
        return Result.success(versionService.findAll());
    }

    @GetMapping("/released")
    public Result<List<DataVersion>> getReleasedVersions() {
        return Result.success(versionService.findAllReleased());
    }

    @PostMapping
    public Result<DataVersion> createVersion() {
        return Result.success(versionService.createVersion());
    }

    @PostMapping("/{id}/release")
    public Result<DataVersion> releaseVersion(@PathVariable Long id, Authentication auth) {
        Long userId = 1L; // 简化处理
        return Result.success(versionService.releaseVersion(id, userId));
    }
}
```

---

### Task 7: DataEntry 实体 + Repository + Service + Controller

**Files:**
- Create: `src/main/java/com/superpower/modules/data/entity/DataEntry.java`
- Create: `src/main/java/com/superpower/modules/data/repository/DataEntryRepository.java`
- Create: `src/main/java/com/superpower/modules/data/dto/DataEntryDTO.java`
- Create: `src/main/java/com/superpower/modules/data/dto/TreeNodeDTO.java`
- Create: `src/main/java/com/superpower/modules/data/service/DataEntryService.java`
- Create: `src/main/java/com/superpower/modules/data/controller/DataEntryController.java`

- [ ] **Step 1: Create DataEntry.java**

```java
package com.superpower.modules.data.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_entry")
public class DataEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_leaf")
    private Boolean isLeaf = true;

    @Column(name = "col_产品系统", length = 500)
    private String colProductSystem;

    @Column(name = "col_应用角色", length = 500)
    private String colAppRole;

    @Column(name = "col_招标参数说明", columnDefinition = "TEXT")
    private String colBidParamDesc;

    @Column(name = "col_功能说明", columnDefinition = "TEXT")
    private String colFeatureDesc;

    @Column(name = "col_状态", length = 100)
    private String colStatus;

    @Column(name = "col_业务分类", length = 200)
    private String colBizCategory;

    @Column(name = "col_业务域", length = 200)
    private String colBizDomain;

    @Column(name = "col_版本划分", length = 200)
    private String colVersionDivision;

    @Column(name = "col_远", length = 50)
    private String colYuan;

    @Column(name = "col_交付工作量人月", length = 100)
    private String colDeliveryWorkload;

    @Column(name = "col_控标点", length = 50)
    private String colControlPoint;

    @Column(name = "col_控标点截图1", columnDefinition = "TEXT")
    private String colControlPointImg1;

    @Column(name = "col_控标点截图2", columnDefinition = "TEXT")
    private String colControlPointImg2;

    @Column(name = "col_控标点截图3", columnDefinition = "TEXT")
    private String colControlPointImg3;

    @Column(name = "col_控标点文档说明", columnDefinition = "TEXT")
    private String colControlPointDoc;

    @Column(name = "col_软著", length = 500)
    private String colCopyright;

    @Column(name = "col_备注", columnDefinition = "TEXT")
    private String colRemark;

    @Column(name = "col_智慧医疗", length = 100)
    private String colSmartMedical;

    @Column(name = "col_智慧服务", length = 100)
    private String colSmartService;

    @Column(name = "col_智慧管理", length = 100)
    private String colSmartManagement;

    @Column(name = "col_互联互通", length = 100)
    private String colInterconnection;

    @Column(name = "col_产品系统标识", length = 100)
    private String colProductSysId;

    @Column(name = "col_模块标识", length = 100)
    private String colModuleId;

    @Column(name = "col_其他解决方案标记", length = 200)
    private String colOtherSolutionTag;

    @Column(name = "col_文档维护人员", length = 100)
    private String colDocMaintainer;

    @Column(name = "col_产品经理", length = 100)
    private String colProductManager;

    @Column(name = "col_父记录", length = 500)
    private String colParentRecord;

    @Column(name = "col_内部版本", length = 100)
    private String colInternalVersion;

    @Column(name = "col_智能化", length = 50)
    private String colIntelligent;

    @Column(name = "col_曜", length = 50)
    private String colYao;

    @Column(name = "col_驰", length = 50)
    private String colChi;

    @Column(name = "col_FY23")
    private BigDecimal colFY23;

    @Column(name = "col_FY24")
    private BigDecimal colFY24;

    @Column(name = "col_FY25")
    private BigDecimal colFY25;

    @Column(name = "col_FY26")
    private BigDecimal colFY26;

    @Column(name = "col_FY27")
    private BigDecimal colFY27;

    @Column(name = "col_FY28")
    private BigDecimal colFY28;

    @Column(name = "col_FY29")
    private BigDecimal colFY29;

    @Column(name = "col_研发成本合计")
    private BigDecimal colRDCostTotal;

    @Column(name = "col_销量曜")
    private Integer colSalesYao;

    @Column(name = "col_销量远")
    private Integer colSalesYuan;

    @Column(name = "col_销量驰")
    private Integer colSalesChi;

    @Column(name = "col_出厂套价保本")
    private BigDecimal colFactoryPrice;

    @Column(name = "col_负责人", length = 200)
    private String colPrincipal;

    @Column(name = "col_产品线", length = 200)
    private String colProductLine;

    @Column(name = "col_资产类型", length = 100)
    private String colAssetType;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by")
    private Long updatedBy;

    public DataEntry cloneWithoutId() {
        DataEntry copy = new DataEntry();
        copy.parentId = this.parentId;
        copy.level = this.level;
        copy.sortOrder = this.sortOrder;
        copy.isLeaf = this.isLeaf;
        copy.colProductSystem = this.colProductSystem;
        copy.colAppRole = this.colAppRole;
        copy.colBidParamDesc = this.colBidParamDesc;
        copy.colFeatureDesc = this.colFeatureDesc;
        copy.colStatus = this.colStatus;
        copy.colBizCategory = this.colBizCategory;
        copy.colBizDomain = this.colBizDomain;
        copy.colVersionDivision = this.colVersionDivision;
        copy.colYuan = this.colYuan;
        copy.colDeliveryWorkload = this.colDeliveryWorkload;
        copy.colControlPoint = this.colControlPoint;
        copy.colControlPointImg1 = this.colControlPointImg1;
        copy.colControlPointImg2 = this.colControlPointImg2;
        copy.colControlPointImg3 = this.colControlPointImg3;
        copy.colControlPointDoc = this.colControlPointDoc;
        copy.colCopyright = this.colCopyright;
        copy.colRemark = this.colRemark;
        copy.colSmartMedical = this.colSmartMedical;
        copy.colSmartService = this.colSmartService;
        copy.colSmartManagement = this.colSmartManagement;
        copy.colInterconnection = this.colInterconnection;
        copy.colProductSysId = this.colProductSysId;
        copy.colModuleId = this.colModuleId;
        copy.colOtherSolutionTag = this.colOtherSolutionTag;
        copy.colDocMaintainer = this.colDocMaintainer;
        copy.colProductManager = this.colProductManager;
        copy.colParentRecord = this.colParentRecord;
        copy.colInternalVersion = this.colInternalVersion;
        copy.colIntelligent = this.colIntelligent;
        copy.colYao = this.colYao;
        copy.colChi = this.colChi;
        copy.colFY23 = this.colFY23;
        copy.colFY24 = this.colFY24;
        copy.colFY25 = this.colFY25;
        copy.colFY26 = this.colFY26;
        copy.colFY27 = this.colFY27;
        copy.colFY28 = this.colFY28;
        copy.colFY29 = this.colFY29;
        copy.colRDCostTotal = this.colRDCostTotal;
        copy.colSalesYao = this.colSalesYao;
        copy.colSalesYuan = this.colSalesYuan;
        copy.colSalesChi = this.colSalesChi;
        copy.colFactoryPrice = this.colFactoryPrice;
        copy.colPrincipal = this.colPrincipal;
        copy.colProductLine = this.colProductLine;
        copy.colAssetType = this.colAssetType;
        return copy;
    }
}
```

- [ ] **Step 2: Create DataEntryRepository.java**

```java
package com.superpower.modules.data.repository;

import com.superpower.modules.data.entity.DataEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DataEntryRepository extends JpaRepository<DataEntry, Long> {

    List<DataEntry> findByVersionId(Long versionId);

    List<DataEntry> findByVersionIdAndParentId(Long versionId, Long parentId);

    List<DataEntry> findByVersionIdAndLevel(Long versionId, Integer level);

    List<DataEntry> findByVersionIdAndParentIdOrderBySortOrder(Long versionId, Long parentId);

    @Query("SELECT e FROM DataEntry e WHERE e.versionId = :versionId AND e.level = 3 " +
           "AND (:name IS NULL OR e.colProductSystem LIKE %:name%) " +
           "AND (:status IS NULL OR e.colStatus = :status) " +
           "AND (:pm IS NULL OR e.colProductManager LIKE %:pm%) " +
           "AND (:tag IS NULL OR e.colOtherSolutionTag LIKE %:tag%)")
    List<DataEntry> queryEntries(@Param("versionId") Long versionId,
                                 @Param("name") String name,
                                 @Param("status") String status,
                                 @Param("pm") String productManager,
                                 @Param("tag") String tag);

    long countByVersionIdAndLevel(Long versionId, Integer level);
}
```

- [ ] **Step 3: Create DataEntryDTO.java**

```java
package com.superpower.modules.data.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DataEntryDTO {
    private Long id;
    private Long versionId;
    private Long parentId;
    private Integer level;
    private Integer sortOrder;
    private Boolean isLeaf;

    private String colProductSystem;
    private String colAppRole;
    private String colBidParamDesc;
    private String colFeatureDesc;
    private String colStatus;
    private String colBizCategory;
    private String colBizDomain;
    private String colVersionDivision;
    private String colYuan;
    private String colDeliveryWorkload;
    private String colControlPoint;
    private String colControlPointImg1;
    private String colControlPointImg2;
    private String colControlPointImg3;
    private String colControlPointDoc;
    private String colCopyright;
    private String colRemark;
    private String colSmartMedical;
    private String colSmartService;
    private String colSmartManagement;
    private String colInterconnection;
    private String colProductSysId;
    private String colModuleId;
    private String colOtherSolutionTag;
    private String colDocMaintainer;
    private String colProductManager;
    private String colParentRecord;
    private String colInternalVersion;
    private String colIntelligent;
    private String colYao;
    private String colChi;
    private BigDecimal colFY23;
    private BigDecimal colFY24;
    private BigDecimal colFY25;
    private BigDecimal colFY26;
    private BigDecimal colFY27;
    private BigDecimal colFY28;
    private BigDecimal colFY29;
    private BigDecimal colRDCostTotal;
    private Integer colSalesYao;
    private Integer colSalesYuan;
    private Integer colSalesChi;
    private BigDecimal colFactoryPrice;
    private String colPrincipal;
    private String colProductLine;
    private String colAssetType;
}
```

- [ ] **Step 4: Create TreeNodeDTO.java**

```java
package com.superpower.modules.data.dto;

import lombok.Data;
import java.util.List;

@Data
public class TreeNodeDTO {
    private Long id;
    private Long parentId;
    private Integer level;
    private String label;
    private Integer sortOrder;
    private Boolean isLeaf;
    private List<TreeNodeDTO> children;
}
```

- [ ] **Step 5: Create DataEntryService.java**

```java
package com.superpower.modules.data.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.data.dto.DataEntryDTO;
import com.superpower.modules.data.dto.TreeNodeDTO;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DataEntryService {

    private final DataEntryRepository entryRepository;

    public DataEntryService(DataEntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    public List<TreeNodeDTO> getTree(Long versionId) {
        List<DataEntry> entries = entryRepository.findByVersionIdAndLevel(versionId, 1);
        return entries.stream().map(e -> buildTree(e, versionId)).toList();
    }

    private TreeNodeDTO buildTree(DataEntry entry, Long versionId) {
        TreeNodeDTO node = new TreeNodeDTO();
        node.setId(entry.getId());
        node.setParentId(entry.getParentId());
        node.setLevel(entry.getLevel());
        node.setLabel(entry.getColProductSystem() != null ? entry.getColProductSystem() : entry.getColBizCategory());
        node.setSortOrder(entry.getSortOrder());
        node.setIsLeaf(entry.getIsLeaf());

        if (entry.getLevel() < 3) {
            List<DataEntry> children = entryRepository.findByVersionIdAndParentIdOrderBySortOrder(versionId, entry.getId());
            if (!children.isEmpty()) {
                node.setChildren(children.stream().map(c -> buildTree(c, versionId)).toList());
            }
        }
        return node;
    }

    public List<DataEntry> getChildren(Long versionId, Long parentId) {
        return entryRepository.findByVersionIdAndParentIdOrderBySortOrder(versionId, parentId);
    }

    public DataEntry getById(Long id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("数据条目不存在"));
    }

    public List<DataEntry> query(Long versionId, String name, String status, String productManager, String tag) {
        return entryRepository.queryEntries(versionId, name, status, productManager, tag);
    }

    @Transactional
    public DataEntry create(DataEntryDTO dto) {
        DataEntry entry = new DataEntry();
        copyFields(entry, dto);
        entry.setVersionId(dto.getVersionId());
        entry.setParentId(dto.getParentId());
        entry.setLevel(dto.getLevel());
        entry.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        entry.setIsLeaf(true);

        // Update parent's isLeaf to false
        if (dto.getParentId() != null) {
            DataEntry parent = entryRepository.findById(dto.getParentId()).orElse(null);
            if (parent != null && parent.getIsLeaf()) {
                parent.setIsLeaf(false);
                entryRepository.save(parent);
            }
        }

        return entryRepository.save(entry);
    }

    @Transactional
    public DataEntry update(Long id, DataEntryDTO dto) {
        DataEntry entry = getById(id);
        copyFields(entry, dto);
        return entryRepository.save(entry);
    }

    @Transactional
    public void delete(Long id) {
        DataEntry entry = getById(id);
        List<DataEntry> children = entryRepository.findByVersionIdAndParentId(entry.getVersionId(), id);
        if (!children.isEmpty()) {
            throw new BusinessException("该节点下有子节点，无法删除");
        }
        entryRepository.deleteById(id);
    }

    private void copyFields(DataEntry entry, DataEntryDTO dto) {
        entry.setColProductSystem(dto.getColProductSystem());
        entry.setColAppRole(dto.getColAppRole());
        entry.setColBidParamDesc(dto.getColBidParamDesc());
        entry.setColFeatureDesc(dto.getColFeatureDesc());
        entry.setColStatus(dto.getColStatus());
        entry.setColBizCategory(dto.getColBizCategory());
        entry.setColBizDomain(dto.getColBizDomain());
        entry.setColVersionDivision(dto.getColVersionDivision());
        entry.setColYuan(dto.getColYuan());
        entry.setColDeliveryWorkload(dto.getColDeliveryWorkload());
        entry.setColControlPoint(dto.getColControlPoint());
        entry.setColControlPointImg1(dto.getColControlPointImg1());
        entry.setColControlPointImg2(dto.getColControlPointImg2());
        entry.setColControlPointImg3(dto.getColControlPointImg3());
        entry.setColControlPointDoc(dto.getColControlPointDoc());
        entry.setColCopyright(dto.getColCopyright());
        entry.setColRemark(dto.getColRemark());
        entry.setColSmartMedical(dto.getColSmartMedical());
        entry.setColSmartService(dto.getColSmartService());
        entry.setColSmartManagement(dto.getColSmartManagement());
        entry.setColInterconnection(dto.getColInterconnection());
        entry.setColProductSysId(dto.getColProductSysId());
        entry.setColModuleId(dto.getColModuleId());
        entry.setColOtherSolutionTag(dto.getColOtherSolutionTag());
        entry.setColDocMaintainer(dto.getColDocMaintainer());
        entry.setColProductManager(dto.getColProductManager());
        entry.setColParentRecord(dto.getColParentRecord());
        entry.setColInternalVersion(dto.getColInternalVersion());
        entry.setColIntelligent(dto.getColIntelligent());
        entry.setColYao(dto.getColYao());
        entry.setColChi(dto.getColChi());
        entry.setColFY23(dto.getColFY23());
        entry.setColFY24(dto.getColFY24());
        entry.setColFY25(dto.getColFY25());
        entry.setColFY26(dto.getColFY26());
        entry.setColFY27(dto.getColFY27());
        entry.setColFY28(dto.getColFY28());
        entry.setColFY29(dto.getColFY29());
        entry.setColRDCostTotal(dto.getColRDCostTotal());
        entry.setColSalesYao(dto.getColSalesYao());
        entry.setColSalesYuan(dto.getColSalesYuan());
        entry.setColSalesChi(dto.getColSalesChi());
        entry.setColFactoryPrice(dto.getColFactoryPrice());
        entry.setColPrincipal(dto.getColPrincipal());
        entry.setColProductLine(dto.getColProductLine());
        entry.setColAssetType(dto.getColAssetType());
    }
}
```

- [ ] **Step 6: Create DataEntryController.java**

```java
package com.superpower.modules.data.controller;

import com.superpower.common.Result;
import com.superpower.modules.data.dto.DataEntryDTO;
import com.superpower.modules.data.dto.TreeNodeDTO;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.service.DataEntryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/data")
public class DataEntryController {

    private final DataEntryService dataEntryService;

    public DataEntryController(DataEntryService dataEntryService) {
        this.dataEntryService = dataEntryService;
    }

    @GetMapping("/tree/{versionId}")
    public Result<List<TreeNodeDTO>> getTree(@PathVariable Long versionId) {
        return Result.success(dataEntryService.getTree(versionId));
    }

    @GetMapping("/children/{versionId}/{parentId}")
    public Result<List<DataEntry>> getChildren(@PathVariable Long versionId,
                                                @PathVariable Long parentId) {
        return Result.success(dataEntryService.getChildren(versionId, parentId));
    }

    @GetMapping("/{id}")
    public Result<DataEntry> getById(@PathVariable Long id) {
        return Result.success(dataEntryService.getById(id));
    }

    @GetMapping("/query/{versionId}")
    public Result<List<DataEntry>> query(
            @PathVariable Long versionId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String productManager,
            @RequestParam(required = false) String tag) {
        return Result.success(dataEntryService.query(versionId, name, status, productManager, tag));
    }

    @PostMapping
    public Result<DataEntry> create(@RequestBody DataEntryDTO dto) {
        return Result.success(dataEntryService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<DataEntry> update(@PathVariable Long id, @RequestBody DataEntryDTO dto) {
        return Result.success(dataEntryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dataEntryService.delete(id);
        return Result.success();
    }
}
```

---

### Task 8: 数据初始化 + 启动验证

**Files:**
- Modify: `src/main/java/com/superpower/SuperPowerApplication.java` (增加 CommandLineRunner)
- Modify: `src/main/resources/application.yml` (增加初始化配置)

- [ ] **Step 1: 添加初始化数据逻辑**

修改 `SuperPowerApplication.java`，在启动时初始化默认角色和用户：

```java
package com.superpower;

import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.system.entity.SysRole;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.repository.SysRoleRepository;
import com.superpower.modules.system.repository.SysUserRepository;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class SuperPowerApplication implements CommandLineRunner {

    private final SysRoleRepository roleRepository;
    private final SysUserRepository userRepository;
    private final DataVersionRepository versionRepository;
    private final DataEntryRepository entryRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperPowerApplication(SysRoleRepository roleRepository,
                                  SysUserRepository userRepository,
                                  DataVersionRepository versionRepository,
                                  DataEntryRepository entryRepository,
                                  PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.versionRepository = versionRepository;
        this.entryRepository = entryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static void main(String[] args) {
        SpringApplication.run(SuperPowerApplication.class, args);
    }

    @Override
    public void run(String... args) {
        if (roleRepository.count() > 0) return;

        // Create roles
        SysRole adminRole = new SysRole();
        adminRole.setName("管理员");
        adminRole.setCode("ADMIN");
        adminRole.setDescription("系统管理员");
        roleRepository.save(adminRole);

        SysRole userRole = new SysRole();
        userRole.setName("普通用户");
        userRole.setCode("USER");
        userRole.setDescription("数据维护人员");
        roleRepository.save(userRole);

        SysRole advancedRole = new SysRole();
        advancedRole.setName("高级用户");
        advancedRole.setCode("ADVANCED");
        advancedRole.setDescription("高级用户，可查询和生成文档");
        roleRepository.save(advancedRole);

        // Create default users (password: 123456)
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setNickname("管理员");
        admin.setRole(adminRole);
        admin.setStatus(1);
        userRepository.save(admin);

        SysUser user1 = new SysUser();
        user1.setUsername("user");
        user1.setPassword(passwordEncoder.encode("123456"));
        user1.setNickname("普通用户");
        user1.setRole(userRole);
        user1.setStatus(1);
        userRepository.save(user1);

        SysUser advanced = new SysUser();
        advanced.setUsername("advanced");
        advanced.setPassword(passwordEncoder.encode("123456"));
        advanced.setNickname("高级用户");
        advanced.setRole(advancedRole);
        advanced.setStatus(1);
        userRepository.save(advanced);

        // Create initial version
        DataVersion version = new DataVersion();
        version.setVersionNo("1.0");
        version.setStatus("draft");
        versionRepository.save(version);
    }
}
```

- [ ] **Step 2: 启动验证**

Run: `mvn spring-boot:run`
Expected: 应用启动成功，数据库创建，默认用户和角色初始化

Test login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```
Expected: 返回 JWT token

Test query versions:
```bash
curl http://localhost:8080/api/versions \
  -H "Authorization: Bearer <token>"
```
Expected: 返回版本列表 [{versionNo: "1.0", status: "draft"}]

Test get tree:
```bash
curl http://localhost:8080/api/data/tree/1 \
  -H "Authorization: Bearer <token>"
```
Expected: 返回空树（暂无数据）
