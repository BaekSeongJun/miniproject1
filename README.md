# 약값알림 — 약국별 일반의약품 최저가 추천 서비스

> 이 README는 T-01(셋업)에서 골격만 작성되며, T-37(마감)에서 완성됩니다.

## 프로젝트 구조

```
miniProject1/
├─ PRD.md            요구사항 정의
├─ DATABASE.md        DB 스키마
├─ API.md             API 명세
├─ ROADMAP.md         태스크 분해
├─ .env.example       환경변수 목록
├─ miniproject1-frontend/     Next.js 16 + React 19.3
├─ miniproject1-backend/      Spring Boot 4.1 + Java 21 + Maven
└─ tools/seed-generator/      공공데이터 → 시드 SQL 변환 (Python)
```

## 개발 환경

- 로컬 PostgreSQL 17 (Docker 미사용)
- 백엔드: `./mvnw spring-boot:run` (`miniproject1-backend/`)
- 프론트: `npm run dev` (`miniproject1-frontend/`)

## 로컬 환경 준비

> T-02에서 채워집니다.

## 커밋 컨벤션

`<type>(<task-id>): <설명>`

- type: `feat` / `fix` / `refactor` / `docs` / `test` / `chore`
- task-id: ROADMAP.md의 태스크 번호 (예: `T-09`)
- 설명: 한글, 현재형

예: `feat(T-09): 거리 계산 컴포넌트 추가`

## 브랜치 컨벤션

`feature/기능명` · `fix/버그명` · `dev/개발명` · `hotfix/수정명`

## 관련 문서

- [PRD.md](./PRD.md) — 요구사항
- [DATABASE.md](./DATABASE.md) — DB 스키마
- [API.md](./API.md) — API 명세
- [ROADMAP.md](./ROADMAP.md) — 태스크 분해
