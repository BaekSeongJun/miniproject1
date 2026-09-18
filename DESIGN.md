---
name: 우리동네약값
description: 동네 약국 조제라벨의 크림 종이와 녹십자 그린을 그대로 옮긴 약값 비교 서비스
colors:
  cream-paper: "oklch(0.975 0.012 85)"
  ink-brown: "oklch(0.24 0.02 50)"
  card-cream: "oklch(0.99 0.008 85)"
  cross-green: "oklch(0.46 0.125 155)"
  secondary-tan: "oklch(0.94 0.02 80)"
  muted-tan: "oklch(0.48 0.025 55)"
  mint-accent: "oklch(0.9 0.05 145)"
  clay-red: "oklch(0.55 0.2 25)"
  border-tan: "oklch(0.85 0.025 75)"
typography:
  body:
    fontFamily: "Gowun Dodum, sans-serif"
    fontWeight: 400
  display:
    fontFamily: "Gowun Dodum, sans-serif"
    fontWeight: 700
rounded:
  sm: "calc(0.875rem * 0.6)"
  md: "calc(0.875rem * 0.8)"
  lg: "0.875rem"
  xl: "calc(0.875rem * 1.4)"
  2xl: "calc(0.875rem * 1.8)"
components:
  button-primary:
    backgroundColor: "{colors.cross-green}"
    textColor: "{colors.card-cream}"
    rounded: "{rounded.lg}"
  badge-recommended:
    backgroundColor: "{colors.cross-green}"
    textColor: "{colors.card-cream}"
    rounded: "{rounded.2xl}"
---

# Design System: 우리동네약값

## Overview

**Creative North Star: "동네 약국 조제라벨"**

이 서비스는 shadcn 기본 팔레트(무채색 그레이 톤, `oklch(x 0 0)`)를 그대로 쓰던 차갑고 획일적인 인상에서 출발해, 동네 약국 간판과 조제라벨이 실제로 갖고 있는 신뢰의 언어로 옮겨왔다. 크림빛 종이 바탕 위에 약국 녹십자의 채도 높은 그린 하나만 강조색으로 커밋하고, 나머지는 전부 따뜻한 갈색조 중립색으로 채운다. 중장년층을 포함한 전 연령대가 쓰는 서비스이므로, 장식보다 큰 글자·명확한 라벨·높은 대비를 우선한다.

색상 전략은 Restrained(중립 + 단일 강조색)다. 그린은 "추천/승인/저렴함"이라는 하나의 의미만 지니며, 화면 전체에 흩뿌리지 않고 핵심 판단 지점(최저가 뱃지, 선택된 정렬, 주요 버튼)에만 커밋한다.

**Key Characteristics:**
- 배경은 항상 크림(무채색 흰색 아님), 텍스트는 진한 잉크브라운(순검정 아님)
- 강조색은 녹십자 그린 하나뿐 — 다른 곳에 임의로 다른 색(파랑, 검정 배경 뱃지)을 쓰지 않는다
- 카드와 버튼은 둥근 모서리(라벨 모서리 느낌)와 2px 보더로 존재감을 준다
- 가격 숫자는 항상 크고 굵게(tabular-nums)

## Colors

크림 배경 위에 그린 하나를 커밋하는 팔레트. 전문 처방 라벨을 볼 때의 신뢰감을 재현한다.

### Primary
- **녹십자 그린** (`oklch(0.46 0.125 155)`): 주요 버튼, 선택된 정렬/반경 토글, "최저가 추천" 뱃지, 강조 링크, 저렴함을 나타내는 텍스트. 배경 대비 6.4:1로 WCAG AA를 여유 있게 만족한다.

### Neutral
- **크림 페이퍼** (`oklch(0.975 0.012 85)`): 페이지 배경.
- **카드 크림** (`oklch(0.99 0.008 85)`): 카드·팝오버·인풋 배경. 페이지 배경보다 살짝 밝다.
- **잉크 브라운** (`oklch(0.24 0.02 50)`): 본문 텍스트. 배경 대비 15.3:1.
- **탄색 세컨더리** (`oklch(0.94 0.02 80)`): 뱃지·보조 버튼 배경.
- **뮤트 탄색** (`oklch(0.48 0.025 55)`): 보조 텍스트(muted-foreground). 배경 대비 6.1:1.
- **보더 탄색** (`oklch(0.85 0.025 75)`): 카드·인풋·구분선 보더. 2px 두께로 존재감을 준다.
- **민트 액센트** (`oklch(0.9 0.05 145)`): 옅은 그린 배경(accent) — 드래그오버 상태 등 그린의 옅은 변주.
- **클레이 레드** (`oklch(0.55 0.2 25)`): destructive(삭제/에러) 전용. 그린과 절대 혼용하지 않는다.

### Named Rules
**The One Green Rule.** 강조색은 녹십자 그린 하나뿐이다. 상태 뱃지처럼 여러 의미를 구분해야 하는 경우(검토 중=amber, 반려=destructive)를 제외하고, 새 UI 요소에 임의의 다른 색(파랑 링크, 검정 배경 뱃지)을 쓰지 않는다.

## Typography

**Body/Display Font:** Gowun Dodum (고운돋움), fallback sans-serif
**Character:** 둥글고 부드러운 한글 산세리프. weight가 400 하나뿐이라 display 텍스트는 브라우저 fake-bold(700)로 굵기를 낸다 — 진짜 bold 웨이트가 필요해지면 헤딩 전용 폰트를 별도로 추가해야 한다.

### Hierarchy
- **Display** (h1, `text-3xl`~`text-5xl`, tracking-tight): 홈 히어로 카피 등 첫 화면 메시지.
- **Title** (h2/h3, `font-bold`~`font-semibold`): 섹션/카드 제목.
- **Body** (`text-sm`~`text-base`): 본문, 폼 라벨.
- **Price** (`text-xl font-bold tabular-nums`): 가격 숫자 전용 — 이 서비스의 핵심 정보이므로 다른 숫자보다 항상 크고 굵다.
- **Label** (`text-xs`, `text-muted-foreground`): 보조 정보(제보 건수, 갱신일 등).

## Layout

모바일 우선 반응형. 헤더는 `flex-wrap`으로 좁은 화면에서 두 번째 줄로 자연스럽게 넘어가되, 워드마크는 항상 한 줄을 유지한다(`shrink-0`). 좁은 화면(< 640px)에서는 위치 표시 라벨과 사용자 닉네임 텍스트를 숨기고 아이콘/필수 링크만 남긴다.

주요 컨텐츠는 `max-w-2xl`~`max-w-4xl` 컨테이너에 중앙 정렬. 카드 리스트는 세로 스택, 간격은 `gap-3`~`gap-6`.

## Elevation & Depth

플랫에 가까운 시스템. 카드는 `shadow-sm`(옅은 그림자)와 2px 보더의 조합으로 깊이감보다 "인쇄물의 테두리" 느낌을 낸다. 강한 그림자나 글래스모피즘은 쓰지 않는다.

### Named Rules
**The Border-Over-Shadow Rule.** 요소를 구분할 때 그림자보다 2px 보더를 우선한다. 라벨/카드가 인쇄된 느낌을 주기 때문이다.

## Shapes

라운드 모서리 스케일이 기존 shadcn보다 전체적으로 커졌다(`--radius: 0.875rem`, 기존 0.625rem). 검색창·카드·버튼은 `rounded-xl`~`rounded-2xl`, 뱃지·칩·토글은 `rounded-full`. 각진 사각형은 거의 쓰지 않는다 — 라벨과 스탬프 같은 둥근 인상을 유지한다.

## Components

### Buttons
- **Shape:** `rounded-full`(칩형) 또는 `rounded-xl`(폼 제출 버튼)
- **Primary:** 배경 `primary`(그린), 텍스트 `primary-foreground`(크림)
- **Toggle(선택형):** 선택 시 배경을 그린으로 채우고 텍스트를 크림으로(`bg-primary text-primary-foreground`), 비선택 시 투명 배경 + 보더만

### Badges
- **최저가 추천 등 긍정 상태:** 그린 배경 + 크림 텍스트, `rounded-full`
- **경고(검토 중):** amber 계열 유지(그린과 혼동 방지)
- **에러(반려):** destructive 계열

### Cards
- **Corner Style:** `rounded-2xl`
- **Background:** `card`(카드 크림)
- **Border:** 2px, 강조 카드(추천 결과)는 `border-primary`
- **Shadow:** `shadow-sm`만

### Inputs / Search
- **Style:** `rounded-2xl`, 2px 보더, 배경은 `card`
- **Focus:** 보더가 `primary`로 바뀌고 옅은 링(`ring-3 ring-ring/30`)

### Navigation (Header)
- 크림 카드 배경 + 하단 2px 보더. 워드마크는 녹십자 심볼(직접 그린 SVG, `LogoMark`) + 굵은 로고타입.

## Do's and Don'ts

### Do:
- **Do** 강조가 필요한 모든 곳에 `primary`(그린) 토큰을 재사용한다. 새 하드코딩 색상을 추가하지 않는다.
- **Do** 가격 숫자는 `PriceTag` 컴포넌트로 통일해 `tabular-nums`와 굵은 크기를 유지한다.
- **Do** 카드/뱃지에 2px 보더 + `shadow-sm` 조합을 쓴다.
- **Do** 좁은 화면에서 헤더 텍스트를 숨기기 전에 실제 390px 뷰포트로 줄바꿈 여부를 확인한다.

### Don't:
- **Don't** `bg-foreground`/`border-foreground`/`text-emerald-*` 같은 이전 무채색·임의색 하드코딩을 새로 추가하지 않는다 — 전부 `primary`/`accent` 토큰으로 대체됐다.
- **Don't** 그린 외의 새로운 강조색(파랑, 보라 등)을 도입하지 않는다. 상태 구분이 꼭 필요하면 기존 `destructive`/amber 팔레트를 재사용한다.
- **Don't** 시스템 산세리프나 Geist를 본문 폰트로 되돌리지 않는다 — 한글 가독성과 톤 유지를 위해 Gowun Dodum을 쓴다.
- **Don't** 카드 모서리를 각지게(radius 0) 만들지 않는다.
