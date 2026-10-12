# FrameSketch

개인 학습 콘텐츠 설계 AI Agent. 그동안 쓴 글의 목차를 모아 두고, 새 주제를 입력하면
**기존 글과 중복되지 않는 목차**를 생성합니다.

## 배경

학습 내용을 포스팅으로 정리할 때 LLM으로 목차를 뽑을 수 있지만, 기존 글과의 중복을
피하려면 그동안 쓴 글의 목차를 매번 직접 복사해서 붙여넣어야 했습니다. 글이 쌓일수록
이 과정을 관리하기 어려워지고, 같은 주제라도 원하는 학습 깊이에 따라 목차의 범위를
조절할 필요도 있었습니다.

이 반복을 없애고 기존 학습 기록을 계속 활용하기 위해 만들었습니다.

## 동작 흐름

```
주제 입력 → 관련 기존 글 검색 → 중복 내용 분석 → 학습 흐름 설계 → 목차 생성 → 결과 저장
```

## 주요 기능

- **기존 글 등록 및 관리** — 제목과 목차를 저장합니다 (본문은 보관하지 않습니다)
- **관련 글 검색** — 제목·목차·태그에서 전문 검색
- **중복 내용 분석** — 새 주제가 기존 글과 겹치는 부분을 찾아냅니다
- **목차 생성** — 상세도 3단계(간단/보통/상세)를 골라 생성
- **생성 결과 저장** — 만든 목차를 보관하고 다시 꺼내 봅니다

## 기술 스택

| 영역 | 선택 | 이유 |
|---|---|---|
| Backend | Spring Boot 4.1 / Java 21 | 계층별 구조화가 쉬워 콘텐츠 관리·DB·LLM 호출을 분리하기 좋음 |
| Frontend | React 19 + TypeScript / Vite | |
| DB | SQLite | 별도 서버 없이 파일 하나로 관리. 이 규모에 충분 |
| LLM 연동 | LangChain4j 1.20 | Java에서 LLM 호출을 구조화. 추후 RAG 확장 용이 |
| LLM | Google Gemini (`gemini-2.5-flash`) | |

### 한국어 검색을 위한 선택

관련 글 검색에 SQLite FTS5 전문 검색을 쓰면서 토크나이저를 `trigram`으로 지정했습니다.
한국어는 조사가 붙는 교착어라 부분 문자열 매칭이 되는 trigram을 활용했습니다.
다만 trigram은 3글자 미만 질의를 매칭하지 못하므로, `DB`·`OS` 같은 짧은 검색어는
애플리케이션에서 `LIKE`로 보완합니다.

## 시작하기

### 요구사항

- JDK 21 이상
- Node.js 20 이상
- Google Gemini API 키 ([발급](https://aistudio.google.com/apikey))



### 1. 백엔드 실행 (8080)

```bash
cd backend
./mvnw spring-boot:run      # Windows: .\mvnw.cmd spring-boot:run
```

DB 파일(`backend/data/framesketch.db`)과 테이블은 첫 실행 때 자동으로 만들어집니다.

### 2. 프론트엔드 실행 (5173)

```bash
cd frontend
npm install
npm run dev
```

`/api` 요청은 8080으로 프록시됩니다.


## API

### 기존 글 관리

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/posts` | 글 등록 |
| `GET` | `/api/posts` | 목록 조회 (`?q=검색어`로 검색) |
| `GET` | `/api/posts/{id}` | 상세 조회 (목차 포함) |
| `PUT` | `/api/posts/{id}` | 수정 |
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

`level`은 목차의 들여쓰기 깊이입니다 (1이 최상위).

## 프로젝트 구조

```
backend/
  src/main/java/com/framesketch/
    config/      DataSource, CORS 설정
    common/      예외 처리
    health/      기동 점검 엔드포인트
    post/        기존 글 관리 (도메인 · 저장소 · 서비스 · 컨트롤러)
  src/main/resources/
    schema.sql   테이블 및 FTS5 색인 정의
frontend/
  src/           React 화면
```

## 진행 상황

- [x] 프로젝트 구성, DB 스키마, Gemini 연동
- [x] 기존 글 관리 API + 검색
- [ ] 목차 생성 API (중복 분석 → Gemini 호출)
- [ ] 생성 결과 저장·조회 API
- [ ] 프론트엔드 화면

## 추후 확장 계획

- **임베딩 기반 유사 콘텐츠 검색** — 키워드 일치가 아닌 의미적 유사도로 중복 판단
- **콘텐츠 유형 확장** — 블로그 목차 외에 발표 자료, 시험 요약, 보고서 구조까지
- **선수 지식 분석** — 새 주제 학습에 필요한 개념을 기존 글에서 찾아 제시
- **다음 학습 주제 추천** — 지금까지 학습한 내용에서 이어지는 주제 제안
