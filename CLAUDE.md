# FrameSketch

개인 학습 콘텐츠 설계 AI Agent. 기존에 쓴 글의 목차를 모아 두고, 새 주제를 입력하면
중복을 피한 새 목차를 생성한다.

서비스 흐름: 주제 입력 → 관련 기존 글 검색 → 중복 내용 분석 → 학습 흐름 설계 → 목차 생성 → 결과 저장

## 구성

```
backend/    Spring Boot 4.1.1 / Java 21 / Maven (mvnw 포함)
frontend/   React 19 + TypeScript / Vite
```

- DB: SQLite 단일 파일 (`backend/data/framesketch.db`, 자동 생성)
- LLM: Google Gemini (`gemini-2.5-flash`) via LangChain4j 1.20.2

## 실행

```powershell
# 백엔드 (8080). API 키는 backend/.env 에서 읽는다.
cd backend
.\mvnw.cmd spring-boot:run

# 프론트엔드 (5173, /api 는 8080 으로 프록시된다)
cd frontend
npm run dev
```

점검: `GET http://localhost:8080/api/health` → SQLite 버전, FTS5 사용 가능 여부, 글 개수,
Gemini 키 설정 여부(`geminiConfigured`)

## 확정된 설계 결정

이미 정한 사항이므로 다시 묻지 말 것.

- **기존 글은 제목과 목차만 저장한다.** 본문은 보관하지 않는다. 중복 판단도 목차 레벨에서
  한다. LLM 컨텍스트에 여러 글을 한 번에 넣기 쉽고, 추후 임베딩 검색으로 넘어갈 때도
  목차 항목이 자연스러운 검색 단위가 된다.
- **상세도는 3단계 프리셋**(간단 / 보통 / 상세). 각각 목차 깊이와 항목 수를 서버 상수로
  매핑한다. 사용자가 depth 를 직접 입력하지 않는다.
- **관련 글 검색은 SQLite FTS5**. 임베딩 기반 유사도 검색은 추후 확장 과제이며 지금은
  범위 밖이다.
- **FTS5 토크나이저는 `trigram`**. 기본 unicode61 은 공백 단위로만 끊어 "운영체제의"와
  "운영체제"를 다른 토큰으로 본다. 한국어는 조사가 붙는 교착어라 부분 문자열 매칭이 되는
  trigram 쪽 품질이 낫다. 다만 trigram 은 3글자 미만 질의를 매칭하지 못하므로 짧은
  검색어는 애플리케이션에서 LIKE 로 보완한다.
- **DB 접근은 Spring JDBC (`JdbcTemplate`)**, JPA 가 아니다. SQLite 는 Hibernate 방언이
  커뮤니티 모듈이고, FTS5 질의는 어차피 네이티브 SQL 로 써야 한다.
- **`DataSource` 는 직접 빈으로 정의한다**(`config/DatabaseConfig`). SQLite 파일이 놓일
  디렉터리를 미리 만들어야 하고, 쓰기 직렬화 때문에 커넥션 풀을 1로 묶는다.

## 프론트엔드 작성 원칙

- AI 및 비즈니스 로직은 프론트엔드에 두지 않는다. 전부 백엔드 REST API 에서 처리한다.
- 컴포넌트는 재사용 가능하게 분리한다.
- API 호출 로직은 별도 파일로 분리한다 (`src/api/`).
- 로딩 / 에러 / 빈 데이터 세 가지 상태를 항상 처리한다.
- 불필요한 외부 라이브러리를 쓰지 않는다. 새 라이브러리가 필요하면 **사용 이유를 먼저
  설명하고 동의를 받는다.** 현재 승인된 것: `react-router-dom`.
- 구현 후에는 변경한 파일과 각 파일의 역할을 설명한다.

## 주의

- `GEMINI_API_KEY` 를 코드나 `application.properties` 에 적지 않는다. 그 파일은 커밋되는
  대상이다. 키는 `backend/.env` (gitignore 대상) 또는 OS 환경 변수로만 다루며, 둘 다 있으면
  환경 변수가 우선한다. 키가 없어도 애플리케이션은 기동되고 LLM 기능만 비활성화된다.
- 새 환경에서 받아 쓸 때는 `backend/.env` 를 직접 만든다. 필요한 값은 이 한 줄뿐이다
  (따옴표로 감싸지 않는다). 키 발급: https://aistudio.google.com/apikey

  ```
  GEMINI_API_KEY=발급받은_키
  ```
- `.env` 는 **BOM 없는 UTF-8** 이어야 한다. PowerShell 의 `Set-Content -Encoding utf8` 은
  BOM 을 붙이는데, 그러면 첫 줄 키가 `﻿GEMINI_API_KEY` 로 파싱되어 조용히 무시된다.
  VS Code 에서 만들면 기본이 BOM 없는 UTF-8 이라 문제없다.
- 반대로 한글이 든 `.ps1` 스크립트는 **BOM 이 있어야** 한다. Windows PowerShell 5.1 은
  BOM 없는 `.ps1` 을 ANSI(MS949) 로 읽어 한글이 깨지고 구문 오류가 난다. 같은 UTF-8 인데
  파일 종류에 따라 요구가 반대라는 점을 기억할 것.
- 키가 실제로 읽혔는지는 `/api/health` 의 `geminiConfigured` 로 확인한다. 이 응답에 키 값
  자체를 싣지 않는다.
- `schema.sql` 은 매 기동마다 실행된다. 모든 문장은 `IF NOT EXISTS` 여야 한다.
- `application.properties` 의 주석은 영어로 쓴다 (Windows 에서 인코딩이 깨진 전례가 있다).
  Java / SQL 소스는 UTF-8 이므로 한글 주석을 써도 된다.

## 구현 현황

**완료** — 개발 환경, 프로젝트 골격, DB 스키마, 기동 점검 엔드포인트, Gemini 키 연동

### 1단계 · 기존 글 관리 API (키 불필요) — 완료

- [x] `Post` / `PostSection` 도메인 + 요청·응답 DTO
- [x] `PostRepository` — 글 등록·조회·수정·삭제 (JdbcTemplate)
- [x] FTS5 색인 동기화 — 글이 바뀔 때 `posts_fts` 갱신
- [x] 검색 — FTS5 전문 검색 + 3글자 미만 검색어 `LIKE` 폴백
- [x] `PostController` — REST 엔드포인트 5종
- [x] 공통 예외 처리 — 없는 글 조회, 입력값 검증 실패

API: `POST /api/posts` · `GET /api/posts?q=` · `GET /api/posts/{id}` ·
`PUT /api/posts/{id}` · `DELETE /api/posts/{id}`

### 2단계 · 목차 생성 API (키 필요)

- [ ] `DetailLevel` enum — 간단/보통/상세를 깊이·항목 수로 매핑
- [ ] `GeminiConfig` — LangChain4j `ChatModel` 빈 (키 없으면 비활성화)
- [ ] 관련 글 검색 → 중복 내용 분석 → 프롬프트 구성
- [ ] Gemini 호출 + JSON 응답 파싱 (형식 깨짐 대비)
- [ ] 생성 결과 저장 / 목록 / 삭제 API

### 3단계 · 프론트엔드

- [ ] `src/api/` — API 호출 모듈 분리
- [ ] 공통 컴포넌트 — 로딩 · 에러 · 빈 상태
- [ ] 라우팅 + 레이아웃
- [ ] 글 목록 화면 — 검색, 삭제
- [ ] 글 등록 화면 — 목차 항목 동적 추가/삭제
- [ ] 목차 생성 화면 — 주제 입력, 상세도 선택, 결과 표시, 저장
- [ ] 저장된 목차 화면 — 조회, 복사, 삭제

### 4단계 · 마무리

- [ ] 주요 흐름 통합 테스트
- [ ] README (설치·실행 안내)
- [ ] 전체 흐름 수동 점검

### 아직 정하지 않은 것

2단계의 **중복 판단 기준**. 같은 목차 제목인지로 볼지 같은 개념을 다루는지로 볼지,
중복을 발견하면 항목에서 빼버릴지 "이미 다룸" 표시만 남길지. 2단계 착수 시 결정한다.
