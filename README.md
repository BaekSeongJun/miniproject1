# 우리동네약값 — 약국별 일반의약품 최저가 추천 서비스

약국마다 자율적으로 가격을 정하는 일반의약품(OTC)의 실거래가를 사용자 제보로 모아, 위치 기반 통계 최저가 추천을 제공하는 미니 프로젝트입니다.

> **이 서비스의 가격은 학습용 예시 데이터입니다.** 시드 데이터(전국 약국 23,571개 · 약품 4,762종 · 가격 제보 약 376만 건)로 크라우드소싱 추천 로직의 동작을 보여주는 데모이며, 실제 약국 판매가와 무관합니다.

## 요구 사항

- JDK 21
- Node.js 22
- PostgreSQL 17

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
- 백엔드: Spring Boot 4.1 + Java 21 + Maven
- 프론트: Next.js 16(App Router) + React 19 + TypeScript strict

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

### 7. 백엔드 실행 시 .env 로드

`application.yml`의 `POSTGRES_*` 기본값은 예시(`changeme`)이므로, 실제 `.env` 값과 다르면 `./mvnw spring-boot:run`/`test`가 DB 인증에 실패한다. 실행 전 `.env`를 셸 환경변수로 로드한다.

```bash
# Git Bash / macOS / Linux
set -a; source .env; set +a
./mvnw spring-boot:run
```

```powershell
# PowerShell
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*([^#=]+)=(.*)$') { Set-Item "Env:$($matches[1].Trim())" $matches[2].Trim() }
}
.\mvnw.cmd spring-boot:run
```

## 실행

터미널 2개가 필요합니다. 첫 실행은 Flyway가 `V1`~`V5` 마이그레이션과 시드 데이터를 자동 적용하므로 몇 분 걸릴 수 있습니다.

**터미널 1 — 백엔드** (`miniproject1-backend/`, 위 "7. 백엔드 실행 시 .env 로드" 절차로 환경변수를 먼저 로드)

```bash
./mvnw spring-boot:run     # http://localhost:8080
```

**터미널 2 — 프론트엔드** (`miniproject1-frontend/`)

```bash
npm install
npm run dev                # http://localhost:3000
```

그 외 자주 쓰는 명령:

```bash
npm run build               # 프로덕션 빌드
npm run lint                # ESLint
npm run test                # vitest
npm run gen:api             # 백엔드 OpenAPI 스펙에서 types/api.ts 재생성 (백엔드 기동 중이어야 함)
```

## 환경변수

`.env.example`을 `.env`로 복사해 값을 채웁니다. ([.env.example](./.env.example))

| 변수 | 설명 | 비고 |
| --- | --- | --- |
| `POSTGRES_HOST` | DB 호스트 | 로컬 개발은 `localhost` |
| `POSTGRES_PORT` | DB 포트 | 기본 `5432` |
| `POSTGRES_DB` | DB 이름 | `pharmaprice` |
| `POSTGRES_USER` | DB 계정 | "2. 계정 및 데이터베이스 생성" 참고 |
| `POSTGRES_PASSWORD` | DB 비밀번호 | 예시값(`changeme`) 그대로 쓰지 말 것 |
| `JWT_SECRET` | JWT 서명 키 | 최소 32바이트 랜덤 문자열 |
| `TZ` | 서버 타임존 | `Asia/Seoul` 고정 |
| `NEXT_PUBLIC_API_BASE_URL` | 프론트가 호출할 백엔드 주소 | 로컬은 `http://localhost:8080` |
| `NEXT_PUBLIC_KAKAO_MAP_KEY` | 카카오맵 JavaScript 키 | [카카오 개발자 콘솔](https://developers.kakao.com/)에서 발급, 플랫폼에 `localhost` 도메인 등록 필요 |
| `PHARMACY_API_KEY` | 공공데이터포털 약국 정보 API 키 | `tools/seed-generator` 전용, 앱 실행에는 불필요 |
| `DRUG_API_KEY` | 식약처 의약품개요정보 API 키 | `tools/seed-generator` 전용, 앱 실행에는 불필요 |

## 기본 계정

시드 데이터에 아래 관리자 계정이 포함되어 있습니다.

| 이메일 | 비밀번호 | 역할 |
| --- | --- | --- |
| `admin@example.com` | `Admin1234!` | ADMIN |

일반 사용자는 `/signup`에서 새로 가입하거나, 검색 후 로그인 없이도 "제보하기"까지는 진입할 수 있습니다(제출 시 로그인 요구).

## 트러블슈팅

| 증상 | 원인 · 해결 |
| --- | --- |
| 백엔드 기동 시 포트 8080 충돌 | 다른 프로세스가 8080을 점유 중. `netstat -ano \| findstr 8080`(Windows) / `lsof -i :8080`으로 확인 후 종료하거나 `application.yml`의 `server.port` 변경 |
| 프론트 기동 시 포트 3000 충돌 | `npm run dev -- -p 3001` 등으로 포트 변경, `NEXT_PUBLIC_API_BASE_URL`도 맞춰서 확인 |
| DB 접속 시 포트 5432 충돌 | "6. .env 생성" 하단 주의사항 참고 — 5433 등으로 변경 후 `.env` 반영 |
| `CREATE EXTENSION pg_trgm` 권한 오류 | 일반 유저는 확장 설치 권한이 없음. "4. pg_trgm 확장 설치"대로 superuser로 미리 설치 |
| 카카오맵이 로드되지 않음("지도를 표시할 수 없습니다" 류) | `NEXT_PUBLIC_KAKAO_MAP_KEY` 미설정이거나, 카카오 개발자 콘솔의 플랫폼 설정에 실행 중인 도메인(`localhost:3000`)이 등록되지 않음 |
| Flyway 체크섬 불일치(`Migration checksum mismatch`) | 이미 적용된 마이그레이션 SQL 파일을 수정함. 로컬 DB라면 `DROP DATABASE` 후 "2. 계정 및 데이터베이스 생성"부터 재실행 |
| JDK 버전 불일치로 빌드 실패 | `java -version`으로 21인지 확인. `./mvnw -v`의 Java 버전도 함께 확인 |
| `ddl-auto: validate` 실패(엔티티/스키마 불일치) | 마이그레이션이 누락되었거나 엔티티가 최신 스키마와 어긋남. `V*.sql` 파일과 `git log`로 최근 스키마 변경을 확인 |

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
- [DEMO.md](./DEMO.md) — 데모 시나리오
