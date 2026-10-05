package com.framesketch.post;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 글 저장소. FTS5 색인(`posts_fts`) 동기화도 함께 책임진다.
 *
 * <p>색인은 트리거가 아니라 애플리케이션에서 갱신한다. 트리거로 하면 `post_sections` 가
 * 여러 행으로 나뉘어 있어 글 하나의 목차 전체를 모아 색인하기가 번거롭고, 글 한 건을 쓸 때
 * 트리거가 여러 번 돌아 색인을 반복해서 다시 쓰게 된다.
 */
@Repository
public class PostRepository {

    /** trigram 토크나이저가 매칭할 수 있는 최소 글자 수. */
    private static final int MIN_FTS_TOKEN_LENGTH = 3;

    private final JdbcTemplate jdbc;

    public PostRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // -----------------------------------------------------------------------
    // 쓰기
    // -----------------------------------------------------------------------

    /** 글을 저장하고 부여된 id 를 돌려준다. 호출부는 트랜잭션 안에서 실행해야 한다. */
    public long insert(String title, String url, String summary,
                       List<PostSection> sections, List<String> tags) {
        jdbc.update(
                "INSERT INTO posts (title, url, summary) VALUES (?, ?, ?)",
                title, url, summary);

        // last_insert_rowid() 는 커넥션 단위 상태다. 호출부가 @Transactional 이라
        // 같은 커넥션이 유지되므로 안전하다.
        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        if (id == null) {
            throw new IllegalStateException("글을 저장했지만 id 를 가져오지 못했습니다.");
        }

        insertSections(id, sections);
        insertTags(id, tags);
        insertFtsIndex(id, title, sections, tags);
        return id;
    }

    /** 글 본문 정보를 바꾸고 목차·태그·색인을 모두 새로 쓴다. 대상이 없으면 false. */
    public boolean update(long id, String title, String url, String summary,
                          List<PostSection> sections, List<String> tags) {
        int affected = jdbc.update(
                "UPDATE posts SET title = ?, url = ?, summary = ?, updated_at = datetime('now') WHERE id = ?",
                title, url, summary, id);
        if (affected == 0) {
            return false;
        }

        // 목차는 순서와 개수가 모두 바뀔 수 있어 부분 갱신보다 전부 지우고 다시 넣는 편이 단순하다.
        jdbc.update("DELETE FROM post_sections WHERE post_id = ?", id);
        jdbc.update("DELETE FROM post_tags WHERE post_id = ?", id);
        jdbc.update("DELETE FROM posts_fts WHERE rowid = ?", id);

        insertSections(id, sections);
        insertTags(id, tags);
        insertFtsIndex(id, title, sections, tags);
        return true;
    }

    /** 글을 지운다. 목차·태그는 외래 키 CASCADE 로 함께 지워지지만 색인은 직접 지워야 한다. */
    public boolean delete(long id) {
        jdbc.update("DELETE FROM posts_fts WHERE rowid = ?", id);
        return jdbc.update("DELETE FROM posts WHERE id = ?", id) > 0;
    }

    private void insertSections(long postId, List<PostSection> sections) {
        List<Object[]> rows = new ArrayList<>(sections.size());
        for (PostSection section : sections) {
            rows.add(new Object[] {postId, section.heading(), section.level(), section.orderIdx()});
        }
        jdbc.batchUpdate(
                "INSERT INTO post_sections (post_id, heading, level, order_idx) VALUES (?, ?, ?, ?)",
                rows);
    }

    private void insertTags(long postId, List<String> tags) {
        // 같은 태그가 중복으로 와도 기본 키 제약에 걸리지 않도록 미리 걸러낸다.
        List<Object[]> rows = new ArrayList<>();
        for (String tag : new LinkedHashSet<>(tags)) {
            rows.add(new Object[] {postId, tag});
        }
        jdbc.batchUpdate("INSERT INTO post_tags (post_id, tag) VALUES (?, ?)", rows);
    }

    private void insertFtsIndex(long postId, String title, List<PostSection> sections, List<String> tags) {
        String headings = String.join("\n", sections.stream().map(PostSection::heading).toList());
        jdbc.update(
                "INSERT INTO posts_fts (rowid, title, headings, tags) VALUES (?, ?, ?, ?)",
                postId, title, headings, String.join(" ", tags));
    }

    // -----------------------------------------------------------------------
    // 읽기
    // -----------------------------------------------------------------------

    public Optional<Post> findById(long id) {
        List<Post> found = loadPosts(List.of(id));
        return found.isEmpty() ? Optional.empty() : Optional.of(found.getFirst());
    }

    public boolean existsById(long id) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM posts WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    /** 최근에 등록한 글부터 돌려준다. */
    public List<Post> findAll(int limit) {
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM posts ORDER BY created_at DESC, id DESC LIMIT ?", Long.class, limit);
        return loadPosts(ids);
    }

    /**
     * 제목·목차·태그에서 검색어를 찾는다.
     *
     * <p>3글자 이상 낱말은 FTS5 색인으로, 그보다 짧은 낱말("DB", "OS" 등)은 LIKE 로 찾는다.
     * trigram 토크나이저가 3글자 단위로 쪼개 색인하기 때문에 짧은 낱말은 색인만으로는
     * 영영 매칭되지 않는다. 두 결과를 합치되 관련도 순인 FTS 결과를 앞에 둔다.
     */
    public List<Post> search(String query, int limit) {
        List<String> words = List.of(query.trim().split("\\s+"));
        List<String> longWords = words.stream().filter(w -> w.length() >= MIN_FTS_TOKEN_LENGTH).toList();
        List<String> shortWords = words.stream()
                .filter(w -> !w.isEmpty() && w.length() < MIN_FTS_TOKEN_LENGTH)
                .toList();

        // LinkedHashSet 으로 FTS 가 매긴 관련도 순서를 유지하면서 중복만 걸러낸다.
        Set<Long> ids = new LinkedHashSet<>();
        if (!longWords.isEmpty()) {
            ids.addAll(searchByFts(longWords, limit));
        }
        if (!shortWords.isEmpty()) {
            ids.addAll(searchByLike(shortWords, limit));
        }

        List<Long> ordered = new ArrayList<>(ids).subList(0, Math.min(ids.size(), limit));
        return loadPosts(ordered);
    }

    private List<Long> searchByFts(List<String> words, int limit) {
        // 낱말마다 따옴표로 감싸 구문으로 만들고 AND 로 잇는다. 감싸지 않으면 사용자가 넣은
        // '*', ':', '-' 같은 글자를 FTS5 가 질의 문법으로 읽어 구문 오류가 난다.
        String match = String.join(" AND ", words.stream()
                .map(word -> '"' + word.replace("\"", "\"\"") + '"')
                .toList());

        return jdbc.queryForList(
                """
                SELECT f.rowid FROM posts_fts f
                WHERE posts_fts MATCH ?
                ORDER BY bm25(posts_fts, 10.0, 5.0, 2.0)
                LIMIT ?
                """,
                Long.class, match, limit);
    }

    private List<Long> searchByLike(List<String> words, int limit) {
        // 낱말 하나라도 걸리면 결과에 포함한다(OR). 짧은 낱말은 보조 수단이라 범위를 넓게 잡는다.
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT p.id FROM posts p
                LEFT JOIN post_sections s ON s.post_id = p.id
                LEFT JOIN post_tags t ON t.post_id = p.id
                WHERE
                """);
        List<Object> params = new ArrayList<>();

        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                sql.append(" OR ");
            }
            sql.append("""
                    (p.title LIKE ? ESCAPE '\\'
                     OR s.heading LIKE ? ESCAPE '\\'
                     OR t.tag LIKE ? ESCAPE '\\')
                    """);
            String pattern = "%" + escapeLike(words.get(i)) + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        sql.append(" ORDER BY p.created_at DESC LIMIT ?");
        params.add(limit);

        return jdbc.queryForList(sql.toString(), Long.class, params.toArray());
    }

    /** LIKE 패턴에서 특수한 뜻을 가지는 글자를 사용자가 입력한 그대로 찾도록 막는다. */
    private String escapeLike(String word) {
        return word.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // -----------------------------------------------------------------------
    // 조립
    // -----------------------------------------------------------------------

    /**
     * id 목록에 해당하는 글을 목차·태그까지 채워 돌려준다. 결과 순서는 넘긴 id 순서를 따른다.
     *
     * <p>글마다 목차와 태그를 따로 조회하면 N+1 질의가 되므로, 세 번의 질의로 모아 읽고
     * 메모리에서 합친다.
     */
    private List<Post> loadPosts(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        Object[] params = ids.toArray();

        Map<Long, PostRow> rows = new LinkedHashMap<>();
        jdbc.query("SELECT id, title, url, summary, created_at, updated_at FROM posts WHERE id IN (" + placeholders + ")",
                (RowMapper<Void>) (rs, rowNum) -> {
                    rows.put(rs.getLong("id"), toPostRow(rs));
                    return null;
                },
                params);

        Map<Long, List<PostSection>> sections = new LinkedHashMap<>();
        jdbc.query("SELECT post_id, heading, level, order_idx FROM post_sections "
                        + "WHERE post_id IN (" + placeholders + ") ORDER BY post_id, order_idx",
                (RowMapper<Void>) (rs, rowNum) -> {
                    sections.computeIfAbsent(rs.getLong("post_id"), key -> new ArrayList<>())
                            .add(new PostSection(rs.getString("heading"), rs.getInt("level"), rs.getInt("order_idx")));
                    return null;
                },
                params);

        Map<Long, List<String>> tags = new LinkedHashMap<>();
        jdbc.query("SELECT post_id, tag FROM post_tags WHERE post_id IN (" + placeholders + ") ORDER BY post_id, tag",
                (RowMapper<Void>) (rs, rowNum) -> {
                    tags.computeIfAbsent(rs.getLong("post_id"), key -> new ArrayList<>())
                            .add(rs.getString("tag"));
                    return null;
                },
                params);

        List<Post> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            PostRow row = rows.get(id);
            if (row == null) {
                continue; // 조회 사이에 지워진 경우
            }
            result.add(new Post(
                    id,
                    row.title(),
                    row.url(),
                    row.summary(),
                    sections.getOrDefault(id, List.of()),
                    tags.getOrDefault(id, List.of()),
                    row.createdAt(),
                    row.updatedAt()));
        }
        return result;
    }

    private PostRow toPostRow(ResultSet rs) throws SQLException {
        return new PostRow(
                rs.getString("title"),
                rs.getString("url"),
                rs.getString("summary"),
                rs.getString("created_at"),
                rs.getString("updated_at"));
    }

    /** posts 한 행. 목차·태그와 합치기 전의 중간 형태다. */
    private record PostRow(String title, String url, String summary, String createdAt, String updatedAt) {}
}
