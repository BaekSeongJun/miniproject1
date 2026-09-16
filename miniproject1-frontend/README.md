# miniproject1-frontend

Next.js 16 (App Router) + React 19 + TypeScript strict 기반 프론트엔드.

## 시작하기

루트 디렉터리의 `.env.example`을 참고해 `miniproject1-frontend/.env.local`을 만든다 (Next.js는 실행 디렉터리 기준으로만 `.env*`를 읽으므로 상위 디렉터리의 `.env`는 읽지 않는다).

```bash
npm run dev
```

[http://localhost:3000](http://localhost:3000) 에서 확인한다.

## 상태 관리 경계

도구가 넷(서버 컴포넌트 fetch, TanStack Query, Redux Toolkit, URL 쿼리)이라 겹치기 쉽다. 아래 기준으로 나눈다 (PRD.md §4.2).

| 상태 종류 | 도구 | 예 |
|---|---|---|
| 초기 렌더에 필요한 서버 데이터 | **서버 컴포넌트에서 직접 fetch** | 검색 결과(`/search`), 약국 상세 |
| 클라이언트에서 다시 불러오는 서버 데이터 | **TanStack Query** | 약품 자동완성, 가격 이력, 제보 후 갱신, 관리자 통계 |
| 화면 간 공유되는 클라이언트 상태 | **Redux Toolkit** | 사용자 위치(`locationSlice`), 인증 세션(`authSlice`), 제보 폼 임시 입력(`reportDraftSlice`) |
| URL에 남아야 하는 상태 | **URL 쿼리** | `drugId`, `lat`, `lng`, `radius`, `sort` — 공유·뒤로가기 동작 |
| 한 컴포넌트 안에서만 쓰는 상태 | `useState` | 드롭다운 열림, 아코디언 펼침 |

> ⚠️ 검색 결과를 TanStack Query로 옮기지 않는다. 서버 컴포넌트 SSR + URL 쿼리가 이미 그 역할을 한다. 둘 다 쓰면 데이터가 두 곳에 생긴다.

`Provider`와 `useAppSelector`/`useAppDispatch`는 클라이언트 컴포넌트에서만 사용한다 (서버 컴포넌트에서 부르면 에러). `app/providers.tsx`가 `QueryClientProvider` + redux `Provider`를 감싸고 루트 레이아웃에 적용되어 있다.

## API 클라이언트

`lib/api.ts`의 `apiFetch<T>()`를 사용한다. 4xx/5xx 응답은 API.md §1.2 에러 포맷을 파싱해 `ApiError`로 던지며, 파싱에 실패해도(HTML 에러 페이지 등) `ApiError`를 던진다. `204 No Content`는 `undefined`를 반환한다.

## 타입 생성

수기 API 타입 정의를 금지한다. 백엔드가 실행 중일 때 아래 명령으로 `/v3/api-docs`에서 타입을 생성한다.

```bash
npm run gen:api
```

> 백엔드 엔티티/컨트롤러가 갖춰지기 전(T-05 이전)에는 의미 있는 스키마가 나오지 않는다.

## 테스트

```bash
npm run test
```
