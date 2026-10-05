-- FrameSketch schema
-- 모든 문장은 IF NOT EXISTS 이므로 매 기동마다 반복 실행해도 안전하다.

-- ---------------------------------------------------------------------------
-- 기존 학습 글: 제목과 목차만 저장한다 (본문은 보관하지 않는다)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS posts (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    title      TEXT NOT NULL,
    url        TEXT,
    summary    TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- 글 하나의 목차 항목. level 은 들여쓰기 깊이(1부터), order_idx 는 표시 순서.
CREATE TABLE IF NOT EXISTS post_sections (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id   INTEGER NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    heading   TEXT    NOT NULL,
    level     INTEGER NOT NULL DEFAULT 1,
    order_idx INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_post_sections_post
    ON post_sections (post_id, order_idx);

CREATE TABLE IF NOT EXISTS post_tags (
    post_id INTEGER NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    tag     TEXT    NOT NULL,
    PRIMARY KEY (post_id, tag)
);

CREATE INDEX IF NOT EXISTS idx_post_tags_tag ON post_tags (tag);

-- ---------------------------------------------------------------------------
-- 전문 검색 인덱스
-- rowid 를 posts.id 와 동일하게 맞춰 저장하므로 조인 키로 바로 쓸 수 있다.
-- tokenize=trigram 을 쓰는 이유: 기본 unicode61 토크나이저는 공백 단위로만
-- 끊어서 "운영체제의" 와 "운영체제" 가 서로 다른 토큰이 된다. 한국어는 조사가
-- 붙는 교착어라 부분 문자열 매칭이 가능한 trigram 쪽이 검색 품질이 훨씬 낫다.
-- 대신 trigram 은 3글자 미만 질의를 매칭하지 못하므로, 짧은 검색어는
-- 애플리케이션에서 LIKE 로 보완한다 (PostRepository 참고).
-- ---------------------------------------------------------------------------
CREATE VIRTUAL TABLE IF NOT EXISTS posts_fts USING fts5 (
    title,
    headings,
    tags,
    tokenize = 'trigram'
);

-- ---------------------------------------------------------------------------
-- 생성된 목차 (생성 결과 저장 기능)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS outlines (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    topic        TEXT NOT NULL,
    detail_level TEXT NOT NULL,
    overlap_note TEXT,
    created_at   TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS outline_items (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    outline_id INTEGER NOT NULL REFERENCES outlines (id) ON DELETE CASCADE,
    heading    TEXT    NOT NULL,
    level      INTEGER NOT NULL DEFAULT 1,
    order_idx  INTEGER NOT NULL,
    note       TEXT
);

CREATE INDEX IF NOT EXISTS idx_outline_items_outline
    ON outline_items (outline_id, order_idx);

-- 목차를 만들 때 참고한 기존 글. 글이 지워져도 생성 이력은 남기려고
-- post_id 에는 FK 를 걸지 않고 제목을 함께 적어 둔다.
CREATE TABLE IF NOT EXISTS outline_references (
    outline_id  INTEGER NOT NULL REFERENCES outlines (id) ON DELETE CASCADE,
    post_id     INTEGER NOT NULL,
    post_title  TEXT    NOT NULL,
    overlap_memo TEXT,
    PRIMARY KEY (outline_id, post_id)
);
