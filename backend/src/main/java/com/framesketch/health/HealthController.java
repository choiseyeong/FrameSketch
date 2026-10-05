package com.framesketch.health;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기동 상태 점검용 엔드포인트.
 *
 * <p>SQLite 연결과 FTS5 전문 검색 모듈 사용 가능 여부까지 확인한다. FTS5 는 SQLite 빌드
 * 옵션에 따라 빠져 있을 수 있어, 검색 기능이 동작하지 않을 때 가장 먼저 확인할 지점이다.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final String geminiApiKey;

    public HealthController(
            JdbcTemplate jdbcTemplate,
            @Value("${framesketch.gemini.api-key}") String geminiApiKey) {
        this.jdbcTemplate = jdbcTemplate;
        this.geminiApiKey = geminiApiKey;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("sqliteVersion", jdbcTemplate.queryForObject("SELECT sqlite_version()", String.class));
        body.put("fts5Available", isFts5Available());
        body.put("postCount", jdbcTemplate.queryForObject("SELECT count(*) FROM posts", Integer.class));
        // 키 설정 여부만 알린다. 값 자체는 어떤 형태로도 응답에 싣지 않는다.
        body.put("geminiConfigured", !geminiApiKey.isBlank());
        return body;
    }

    private boolean isFts5Available() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pragma_compile_options WHERE compile_options LIKE 'ENABLE_FTS5%'",
                Integer.class);
        return count != null && count > 0;
    }
}
