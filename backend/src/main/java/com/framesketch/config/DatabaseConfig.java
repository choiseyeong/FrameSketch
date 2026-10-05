package com.framesketch.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SQLite 전용 DataSource 설정.
 *
 * <p>자동 설정 대신 직접 빈을 만드는 이유는 두 가지다. 첫째, SQLite 는 파일이 놓일 디렉터리가
 * 미리 존재해야 하는데 자동 설정은 그 시점에 디렉터리를 만들어 주지 않는다. 둘째, SQLite 는
 * 쓰기 트랜잭션을 동시에 하나만 허용하므로 커넥션 풀을 1로 묶어 잠금 충돌을 아예 없애는 편이
 * 이 규모에서는 가장 단순하고 안전하다.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private static final String JDBC_SQLITE_PREFIX = "jdbc:sqlite:";

    @Bean
    public DataSource dataSource(@Value("${spring.datasource.url}") String url) {
        createDatabaseDirectory(url);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setPoolName("sqlite-pool");
        // SQLite 의 쓰기는 직렬화되므로 커넥션을 늘려도 이득이 없고 SQLITE_BUSY 만 늘어난다.
        config.setMaximumPoolSize(1);
        // 외래 키 제약은 SQLite 에서 커넥션마다 명시적으로 켜 줘야 한다.
        config.setConnectionInitSql("PRAGMA foreign_keys = ON");

        return new HikariDataSource(config);
    }

    /** jdbc:sqlite:./data/framesketch.db 같은 URL 에서 파일 경로를 뽑아 상위 디렉터리를 만든다. */
    private void createDatabaseDirectory(String url) {
        if (!url.startsWith(JDBC_SQLITE_PREFIX)) {
            return;
        }

        String location = url.substring(JDBC_SQLITE_PREFIX.length());
        int queryStart = location.indexOf('?');
        if (queryStart >= 0) {
            location = location.substring(0, queryStart);
        }
        // 인메모리 DB 는 만들 디렉터리가 없다.
        if (location.isBlank() || location.startsWith(":")) {
            return;
        }

        Path parent = Path.of(location).toAbsolutePath().getParent();
        if (parent == null || Files.isDirectory(parent)) {
            return;
        }

        try {
            Files.createDirectories(parent);
            log.info("데이터베이스 디렉터리를 생성했습니다: {}", parent);
        } catch (IOException e) {
            throw new UncheckedIOException("데이터베이스 디렉터리를 만들 수 없습니다: " + parent, e);
        }
    }
}
