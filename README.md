# FrameSketch

**개인 학습 콘텐츠 설계 AI Agent.** 그동안 쓴 글의 목차를 모아 두고, 새 주제를 입력하면
**기존 글과 중복되지 않는 목차**를 생성합니다.

> 상세도 3단계(간단 / 보통 / 상세)에서 고르면 Gemini 가 관련 글의 목차를 참고해
> 중복을 피한 새 목차를 설계하고, 어떤 글의 어느 부분과 겹치는지 설명까지 함께 돌려줍니다.

## 배경

학습 내용을 포스팅으로 정리할 때 LLM 으로 목차를 뽑을 수 있지만, 기존 글과의 중복을
피하려면 그동안 쓴 글의 목차를 매번 직접 복사해서 붙여넣어야 했습니다. 글이 쌓일수록
이 과정을 관리하기 어려워지고, 같은 주제라도 원하는 학습 깊이에 따라 목차의 범위를
조절할 필요도 있었습니다.

이 반복을 없애고 기존 학습 기록을 계속 활용하기 위해 만들었습니다.

## 동작 흐름

```
주제 입력 → 관련 기존 글 검색 → 중복 내용 분석 → 학습 흐름 설계 → 목차 생성 → 결과 저장
```

## 주요 기능

- **기존 글 등록 및 관리** — 제목·목차·태그를 저장합니다 (본문은 보관하지 않습니다)
- **관련 글 검색** — 제목·목차·태그에 대한 한국어 전문 검색 (FTS5 trigram)
- **중복 내용 분석** — 새 주제와 기존 글이 겹치는 부분을 LLM 이 찾아 설명합니다
- **상세도 선택** — 간단 / 보통 / 상세 중 하나로 목차 깊이와 분량 조절
- **생성 결과 저장 · 마크다운 복사** — 보관한 목차를 바로 블로그에 붙여넣을 수 있는 형태로


## 기술 스택

| 영역 | 선택 | 이유 |
|---|---|---|
| Backend | Spring Boot 4.1 / Java 21 / Maven | 계층별 구조화가 쉬워 콘텐츠·DB·LLM 호출을 분리하기 좋음 |
| Frontend | React 19 + TypeScript / Vite | 빠른 개발 서버, 타입으로 API 응답 구조 안전하게 다룸 |
| DB | SQLite | 별도 서버 없이 파일 하나로 관리. 이 규모에 충분 |
| LLM 연동 | LangChain4j 1.20 | Java 에서 LLM 호출을 구조화. 추후 RAG 확장 용이 |
| LLM | Google Gemini (`gemini-flash-latest`) | 안정 별칭으로 지정해 모델 교체에도 코드 수정이 필요 없음 |

### 한국어 검색을 위한 선택

관련 글 검색에 SQLite **FTS5** 전문 검색을 쓰면서 토크나이저를 `trigram` 으로 지정했습니다.
기본 `unicode61` 은 공백 단위로만 끊어 `"운영체제의"` 와 `"운영체제"` 를 다른 토큰으로 봅니다.
한국어는 조사가 붙는 교착어라 부분 문자열 매칭이 되는 trigram 쪽이 품질이 좋습니다.
다만 trigram 은 3글자 미만 질의를 매칭하지 못하므로, `DB`·`OS` 같은 짧은 검색어는
애플리케이션에서 `LIKE` 로 보완합니다.

### 중복 처리 전략

LLM 에게 관련 글의 목차를 전부 프롬프트에 넣고 처리를 맡깁니다. 응답으로 목차와 함께
`overlapNote` (전체 전략 요약) 와 각 항목의 `note` (어떤 글의 어느 부분과 겹치는지) 를
받아 사용자가 결과를 눈으로 확인할 수 있습니다.

## 시작하기

### 요구사항

- JDK 21 이상
- Node.js 20 이상
- Google Gemini API Key

### 1. 백엔드 실행 (8080)

```bash
cd backend
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

DB 파일(`backend/data/framesketch.db`)과 테이블은 첫 실행 때 자동으로 만들어집니다.

### 2. 프론트엔드 실행 (5173)

```bash
cd frontend
npm install
npm run dev
```

브라우저에서 <http://localhost:5173> 접속. `/api` 요청은 Vite 가 8080 으로 프록시합니다.

### 동작 확인

<http://localhost:8080/api/health>

```json
{
  "status": "UP",
  "sqliteVersion": "3.53.4",
  "fts5Available": true,
  "postCount": 0,
  "geminiConfigured": true
}
```

`fts5Available` 가 `false` 면 검색이 동작하지 않고, `geminiConfigured` 가 `false` 면 API 키를
읽지 못한 것입니다.

## API

### 기존 글 관리

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST`   | `/api/posts` | 글 등록 |
| `GET`    | `/api/posts` | 목록 조회 (`?q=검색어` 로 검색) |
| `GET`    | `/api/posts/{id}` | 상세 조회 (목차 포함) |
| `PUT`    | `/api/posts/{id}` | 수정 |
| `DELETE` | `/api/posts/{id}` | 삭제 |

등록 요청 예시:

```json
{
  "title": "운영체제의 프로세스와 스레드",
  "url": "https://blog.example.com/os-process",
  "tags": ["운영체제", "CS"],
  "sections": [
    { "heading": "프로세스란 무엇인가", "level": 1 },
    { "heading": "프로세스 제어 블록(PCB)", "level": 2 },
    { "heading": "스레드와 멀티스레딩", "level": 1 }
  ]
}
```

`level` 은 목차의 들여쓰기 깊이입니다 (1이 최상위).

### 목차 생성

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST`   | `/api/outlines/generate` | 목차 생성 (저장하지 않음, 재생성 자유) |
| `POST`   | `/api/outlines`          | 생성 결과 저장 |
| `GET`    | `/api/outlines`          | 저장된 목차 목록 |
| `GET`    | `/api/outlines/{id}`     | 상세 조회 |
| `DELETE` | `/api/outlines/{id}`     | 삭제 |

생성 요청 예시:

```json
{
  "topic": "운영체제 메모리 관리",
  "detailLevel": "STANDARD"
}
```

`detailLevel` 은 `SIMPLE` (1단계·약 5개) / `STANDARD` (2단계·약 8개) / `DETAILED`
(3단계·약 12개) 중 하나입니다. 생성 → 저장이 두 단계로 나뉘어 있어 결과를 확인한 뒤에만
저장되고, 마음에 들지 않으면 재생성을 반복해도 DB 가 쌓이지 않습니다.

## 프로젝트 구조

```
backend/
  src/main/java/com/framesketch/
    config/      DataSource, CORS, Gemini ChatModel 설정
    common/      공통 예외 처리
    health/      기동 점검 엔드포인트
    post/        기존 글 관리 (도메인 · 저장소 · 서비스 · 컨트롤러)
    outline/     목차 생성 (프롬프트 조립 · LLM 호출 · 저장소)
  src/main/resources/
    schema.sql   테이블 및 FTS5 색인 정의
frontend/
  src/
    api/         fetch 래퍼 + 엔드포인트별 모듈
    components/  Layout, Button, StatusMessage (공통 컴포넌트)
    pages/       화면 6종
    styles/      디자인 토큰
```

## 추후 확장 계획

- **임베딩 기반 유사 콘텐츠 검색** — 키워드 일치가 아닌 의미적 유사도로 중복 판단
- **콘텐츠 유형 확장** — 블로그 목차 외에 발표 자료, 시험 요약, 보고서 구조까지
- **선수 지식 분석** — 새 주제 학습에 필요한 개념을 기존 글에서 찾아 제시
- **다음 학습 주제 추천** — 지금까지 학습한 내용에서 이어지는 주제 제안
