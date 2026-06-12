package com.superpower.config;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.Statement;

@Configuration
public class SqliteConfig {

    private static final Logger log = LoggerFactory.getLogger(SqliteConfig.class);
    private final DataSource dataSource;

    public SqliteConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA busy_timeout=30000");
            var rs = stmt.executeQuery("PRAGMA journal_mode");
            if (rs.next()) {
                log.info("SQLite journal_mode: {}", rs.getString(1));
            }
            rs = stmt.executeQuery("PRAGMA busy_timeout");
            if (rs.next()) {
                log.info("SQLite busy_timeout: {}ms", rs.getInt(1));
            }
        } catch (Exception e) {
            log.warn("Failed to set SQLite PRAGMA: {}", e.getMessage());
        }
    }
}
