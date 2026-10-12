package com.framesketch.outline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** 생성된 목차의 저장소. 쓰기는 서비스에서 트랜잭션으로 감싼다. */
@Repository
public class OutlineRepository {

    private final JdbcTemplate jdbc;

    public OutlineRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // -----------------------------------------------------------------------
    // 쓰기
    // -----------------------------------------------------------------------

    public long insert(String topic, DetailLevel detailLevel, String overlapNote,
                       List<Outline.Item> items, List<Outline.Reference> references) {
        jdbc.update(
                "INSERT INTO outlines (topic, detail_level, overlap_note) VALUES (?, ?, ?)",
                topic, detailLevel.name(), overlapNote);

        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        if (id == null) {
            throw new IllegalStateException("목차를 저장했지만 id 를 가져오지 못했습니다.");
        }

        insertItems(id, items);
        insertReferences(id, references);
        return id;
    }

    public boolean delete(long id) {
        return jdbc.update("DELETE FROM outlines WHERE id = ?", id) > 0;
    }

    private void insertItems(long outlineId, List<Outline.Item> items) {
        List<Object[]> rows = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            Outline.Item item = items.get(i);
            rows.add(new Object[] {outlineId, item.heading(), item.level(), i, item.note()});
        }
        jdbc.batchUpdate(
                "INSERT INTO outline_items (outline_id, heading, level, order_idx, note) VALUES (?, ?, ?, ?, ?)",
                rows);
    }

    private void insertReferences(long outlineId, List<Outline.Reference> references) {
        if (references.isEmpty()) {
            return;
        }
        List<Object[]> rows = new ArrayList<>(references.size());
        for (Outline.Reference ref : references) {
            rows.add(new Object[] {outlineId, ref.postId(), ref.postTitle(), ref.overlapMemo()});
        }
        jdbc.batchUpdate(
                "INSERT OR IGNORE INTO outline_references "
                        + "(outline_id, post_id, post_title, overlap_memo) VALUES (?, ?, ?, ?)",
                rows);
    }

    // -----------------------------------------------------------------------
    // 읽기
    // -----------------------------------------------------------------------

    public Optional<Outline> findById(long id) {
        List<Outline> found = loadOutlines(List.of(id));
        return found.isEmpty() ? Optional.empty() : Optional.of(found.getFirst());
    }

    public List<Outline> findAll(int limit) {
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM outlines ORDER BY created_at DESC, id DESC LIMIT ?", Long.class, limit);
        return loadOutlines(ids);
    }

    private List<Outline> loadOutlines(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        Object[] params = ids.toArray();

        Map<Long, OutlineRow> rows = new LinkedHashMap<>();
        jdbc.query("SELECT id, topic, detail_level, overlap_note, created_at FROM outlines "
                        + "WHERE id IN (" + placeholders + ")",
                (RowMapper<Void>) (rs, rowNum) -> {
                    rows.put(rs.getLong("id"), new OutlineRow(
                            rs.getString("topic"),
                            rs.getString("detail_level"),
                            rs.getString("overlap_note"),
                            rs.getString("created_at")));
                    return null;
                },
                params);

        Map<Long, List<Outline.Item>> items = new LinkedHashMap<>();
        jdbc.query("SELECT outline_id, heading, level, note FROM outline_items "
                        + "WHERE outline_id IN (" + placeholders + ") ORDER BY outline_id, order_idx",
                (RowMapper<Void>) (rs, rowNum) -> {
                    items.computeIfAbsent(rs.getLong("outline_id"), k -> new ArrayList<>())
                            .add(new Outline.Item(
                                    rs.getString("heading"),
                                    rs.getInt("level"),
                                    rs.getString("note")));
                    return null;
                },
                params);

        Map<Long, List<Outline.Reference>> refs = new LinkedHashMap<>();
        jdbc.query("SELECT outline_id, post_id, post_title, overlap_memo FROM outline_references "
                        + "WHERE outline_id IN (" + placeholders + ") ORDER BY outline_id, post_id",
                (RowMapper<Void>) (rs, rowNum) -> {
                    refs.computeIfAbsent(rs.getLong("outline_id"), k -> new ArrayList<>())
                            .add(new Outline.Reference(
                                    rs.getLong("post_id"),
                                    rs.getString("post_title"),
                                    rs.getString("overlap_memo")));
                    return null;
                },
                params);

        List<Outline> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            OutlineRow row = rows.get(id);
            if (row == null) continue;
            result.add(new Outline(
                    id,
                    row.topic(),
                    parseLevel(row.detailLevel()),
                    row.overlapNote(),
                    refs.getOrDefault(id, List.of()),
                    items.getOrDefault(id, List.of()),
                    row.createdAt()));
        }
        return result;
    }

    /** DB 에 저장된 과거 enum 이름이 지금은 사라졌어도 보통 수준으로 폴백해 조회가 깨지지 않게 한다. */
    private DetailLevel parseLevel(String raw) {
        try {
            return DetailLevel.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return DetailLevel.STANDARD;
        }
    }

    private record OutlineRow(String topic, String detailLevel, String overlapNote, String createdAt) {}
}
