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

### 1. PostgreSQL 17 설치

- **Windows**: [postgresql.org/download/windows](https://www.postgresql.org/download/windows/) 설치 프로그램 실행. Locale은 `C` 또는 `en_US.UTF-8`.
- **macOS**: `brew install postgresql@17 && brew services start postgresql@17`
- **Linux**: 배포판 패키지 또는 PGDG 저장소.

설치되면 Windows는 `postgresql-x64-17` 서비스로 자동 등록·실행된다. `psql`이 PATH에 없다면 `C:\Program Files\PostgreSQL\17\bin\psql.exe`를 직접 사용한다.

### 2. 계정 및 데이터베이스 생성

`postgres` superuser로 접속해 실행한다.

```sql
CREATE USER pharmaprice WITH PASSWORD '실제_비밀번호';
CREATE DATABASE pharmaprice OWNER pharmaprice ENCODING 'UTF8';
\c pharmaprice
GRANT ALL ON SCHEMA public TO pharmaprice;
```

### 3. 타임존을 Asia/Seoul로 설정

```sql
ALTER DATABASE pharmaprice SET timezone TO 'Asia/Seoul';
```

재접속 후 확인:

```bash
psql -h localhost -p 5432 -U pharmaprice -d pharmaprice -c "SHOW timezone;"   # Asia/Seoul
psql -h localhost -p 5432 -U pharmaprice -d pharmaprice -c "SELECT now();"     # +09 오프셋
```

UTC로 두면 한국 시간 오전 9시 이전에 `CURRENT_DATE`가 전날로 잡혀 중복 제보 방지와 180일 검증이 하루씩 어긋난다 ([DATABASE.md](./DATABASE.md) §8).

### 4. pg_trgm 확장 설치

`V1__init.sql`이 `CREATE EXTENSION`으로 설치하지만, 일반 유저는 권한이 없어 실패할 수 있다. 미리 superuser로 설치해둔다.

```sql
\c pharmaprice postgres
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

### 5. 접속 확인

```bash
psql -h localhost -p 5432 -U pharmaprice -d pharmaprice -c "SELECT version();"
```

### 6. .env 생성

`.env.example`을 복사해 `.env`를 만들고 실제 비밀번호로 교체한다.

```bash
cp .env.example .env
```

**주의**: 포트 5432가 이미 다른 PostgreSQL 설치본에 점유돼 있으면 충돌한다. Windows에서는 `Get-Service postgresql*`, macOS/Linux는 `lsof -i :5432`로 확인하고, 필요하면 5433을 쓰되 `.env`에도 반영한다.

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
