# 프론트엔드 아키텍처 (Next.js 15 App Router)

ROAMPASS 프론트(`frontend/`)의 설계 기준이다. Next.js를 처음 쓰는 1년차가 **모든 파일을 설명할 수 있게** 하는 것이 목표라서, 기능을 늘리는 것보다 규칙을 적게, 이유를 분명하게 두었다.
개념이 처음이면 [`docs/learn/L1_Nextjs_기초.md`](../../../docs/learn/L1_Nextjs_기초.md)를 먼저 읽는다. 화면별 서버/클라이언트 구분은 [`docs/08_프론트_경계.md`](../../../docs/08_프론트_경계.md) §2가 기준이다.

## 0. 한 문장 요약

> **페이지는 서버에서 데이터를 채워 보내고, 누르거나 시간이 지나며 바뀌는 작은 부분만 브라우저에서 돈다. 코드는 백엔드와 같은 기능 이름으로 나눈다.**

## 1. 스택과 고르지 않은 것

| 쓰는 것 | 이유 |
|---|---|
| Next.js 15 App Router + TypeScript | 과제 지정. 공식 튜토리얼이 App Router 기준(ADR-01) |
| TanStack Query v5 | 폴링을 "최종 상태면 멈춤" 한 줄로 쓰고, 화면을 떠나면 알아서 멈춘다(ADR-02) |
| `server-only` 패키지 | 서버 전용 파일을 브라우저 코드가 import하면 **빌드가 실패**하게 만든다. 규칙을 기억에 맡기지 않기 위해서다 |
| CSS 파일 하나(`app/globals.css`) | UI 완성도는 평가 항목이 아니다. 배울 도구를 늘리지 않는다 |

| 쓰지 않는 것 | 이유 |
|---|---|
| Tailwind, UI 라이브러리 | 위와 같다. 화면은 기본 HTML + 약간의 CSS |
| Server Actions, Route Handlers(`app/api/*`) | 백엔드 API는 Spring에 이미 있다. Next는 화면만 맡고 `/api/*`는 rewrites로 Spring에 넘긴다. 처음 배우는 개념을 줄인다 |
| middleware | 로그인 시스템이 없다(`X-User-Id` 헤더면 충분, 과제 2.2) |
| 전역 상태 라이브러리(Redux, Zustand) | 서버 데이터는 TanStack Query가, 화면 안 상태는 `useState`가 맡으면 끝난다 |
| e2e·컴포넌트 테스트 | 필수 테스트는 백엔드에서 증명한다. 프론트는 `npm run build`(타입·경계 검사)와 직접 확인으로 본다 |

## 2. 폴더 구조

```
frontend/
├─ app/                          URL = 폴더. 여기에는 "화면 조립"만 둔다
│  ├─ layout.tsx                 모든 화면의 틀 (Server). Providers로 감싼다
│  ├─ providers.tsx              'use client'. QueryClientProvider 하나
│  ├─ globals.css
│  ├─ error.tsx / not-found.tsx  에러·404 화면
│  ├─ page.tsx                   /                       요금제 목록
│  ├─ checkout/[orderNo]/page.tsx
│  ├─ orders/[orderNo]/waiting/page.tsx
│  ├─ my-sims/page.tsx, my-sims/[orderNo]/page.tsx
│  └─ admin/page.tsx, admin/orders/[orderNo]/page.tsx
├─ features/                     백엔드 기능 패키지와 같은 이름
│  └─ {feature}/                 plan, order, payment, issue, admin
│     ├─ types.ts                백엔드 응답 모양 (백엔드 dto와 1:1)
│     ├─ server.ts               서버에서 부르는 조회 함수 (import 'server-only')
│     ├─ client.ts               브라우저에서 부르는 함수 (폴링 조회, 버튼 요청)
│     └─ components/             이 기능의 화면 조각. 브라우저용은 첫 줄에 'use client'
├─ components/                   어느 기능에도 속하지 않는 조각 (KstTime, FreshnessBadge, UserIdForm)
└─ lib/                          기능을 모르는 도구
   ├─ api-error.ts               ApiError + 응답 처리 (ProblemDetail → code, detail)
   ├─ api-server.ts              서버용 fetch, 쿠키의 아이디 읽기 (import 'server-only')
   ├─ api-client.ts              브라우저용 fetch
   ├─ idempotency.ts             멱등키 만들기 (http에서도 동작)
   ├─ user.ts                    아이디 쿠키 이름, 쿠키 저장(브라우저 전용 saveUserId)
   └─ time.ts                    UTC → KST 표시
```

### 왜 이렇게 나눴나
- **백엔드와 같은 기능 이름**(`features/order` ↔ `com.roampass.order`)이라 "이 화면이 부르는 API가 어디 있나"를 바로 찾고, 면접에서 한 기능을 앞뒤로 이어서 설명할 수 있다
- **`app/`에는 조립만 둔다.** 페이지 파일이 길어지면 "어디가 서버이고 어디가 브라우저인지"가 흐려진다. 페이지는 데이터를 가져와 `features/*/components`에 넘기는 일만 한다
- **`server.ts` / `client.ts`를 파일로 나눈다.** 같은 API라도 서버에서 부를 때(`http://backend:8080`, 쿠키를 `cookies()`로 읽음)와 브라우저에서 부를 때(`/api/...`, `document.cookie`)가 다르다. 파일이 다르면 섞일 일이 없고, `server-only`가 실수를 빌드에서 잡는다

## 3. 의존 방향

```
app  ──▶  features/*  ──▶  components  ──▶  lib
  └──────────────────────────┴──────────────▶ lib
```

1. 위에서 아래로만 import한다. `lib`은 아무것도 모르고, `components`는 `lib`만 안다
2. 기능끼리는 **`types.ts`만** 가져다 쓸 수 있다(예: `admin`이 `order`의 상태 타입을 씀). 다른 기능의 컴포넌트나 API 함수가 필요하면 그 화면은 `app/`에서 두 기능을 조립한다
   - 이유: 백엔드에서 "다른 기능은 Service로만"과 같은 생각이다. 기능 사이에 실이 엉키면 한 화면을 고칠 때 다른 화면이 깨진다
3. import 경로는 `@/` 별칭(`@/features/order/types`)으로 쓴다. `../../../`는 쓰지 않는다

## 4. 서버/클라이언트 경계

| 질문 | 예 → | 아니오 → |
|---|---|---|
| 클릭·입력·타이머(폴링)·`useState`가 필요한가? | Client Component (`'use client'`) | 다음 질문 |
| 페이지에 들어왔을 때 보여 줄 데이터인가? | Server Component에서 `features/*/server.ts`로 조회 | 그냥 Server Component |

1. **`page.tsx`와 `layout.tsx`는 항상 Server Component**다. `'use client'`를 붙이지 않는다
2. `'use client'`는 **가장 작은 단위**(버튼, 폴러, 필터 폼)에만 붙인다. 그 파일이 import하는 것도 전부 브라우저로 가기 때문이다
3. 서버에서 받은 첫 데이터는 props(`initial`)로 Client Component에 넘긴다. Client Component는 그 값으로 시작해서 필요할 때만 다시 묻는다 → 첫 화면이 비어 있지 않다
4. Server → Client로 넘기는 props는 **JSON으로 바꿀 수 있는 값**(문자열, 숫자, 배열, 객체)만이다. 함수, `Date` 객체는 넘기지 않는다(시각은 ISO 문자열 그대로 넘긴다)
5. Next 15부터 `params`, `searchParams`는 **Promise**다. `const { orderNo } = await params`

## 5. 데이터 가져오기와 보내기

### 5.1 백엔드 연결
```
브라우저 ──fetch('/api/...')──▶ Next 서버 ──rewrites──▶ Spring (API_INTERNAL_URL)
Server Component ──fetch(API_INTERNAL_URL + '/api/...')──▶ Spring
```
- `next.config.ts`의 rewrites가 `/api/*`를 Spring으로 넘긴다. 브라우저 입장에서 같은 도메인이라 **CORS 설정이 필요 없다**
- `API_INTERNAL_URL`은 서버에서만 읽는다. **`NEXT_PUBLIC_`을 붙이지 않는다**(붙이면 빌드 결과에 박혀 브라우저에 노출된다)

### 5.2 규칙
1. **모든 fetch는 `cache: 'no-store'`**, 데이터를 읽는 페이지에는 `export const dynamic = 'force-dynamic'`. 결제·발급 상태가 캐시돼 옛날 값이 보이면 사고다(docs/08 §6)
2. fetch는 `lib/api-server.ts`·`lib/api-client.ts`를 통해서만 한다. 컴포넌트에서 `fetch()`를 직접 부르지 않는다
   - 이유: 사용자 헤더, 캐시 끄기, 에러 해석을 한 곳에서 한다. 빠뜨릴 곳이 없다
3. 에러는 `ApiError(status, code, detail)` 하나로 바뀐다. 백엔드 `ProblemDetail`의 `code`로 분기하고, 사용자에게는 `detail`(한글 메시지)을 보여 준다
   - 서버 페이지에서 404면 `notFound()`를 불러 `not-found.tsx`를 보여 준다
   - 그 밖에 서버에서 처리하지 못한 에러는 `app/error.tsx`가 보여 준다. 운영 빌드에서 Next는 서버 에러 메시지를 숨기고 `digest`(로그 추적 번호)만 넘기므로 고정 문구 + digest를 보여 준다. 사용자에게 이유를 알려야 하는 에러(결제 불가 등)는 페이지나 컴포넌트에서 `ApiError`를 직접 처리한다
4. 사용자 식별: 첫 화면에서 입력한 아이디를 쿠키(`roampass_user`)에 두고, 서버는 `cookies()`(`getUserId`), 브라우저는 `document.cookie`로 읽어 `X-User-Id` 헤더에 붙인다. 아이디가 없는데 `/api/my/*`를 조회하려 하면 `serverGet`이 첫 화면(`/`)으로 보낸다(`redirect`). 헤더에는 한글을 넣을 수 없어서 아이디는 영문·숫자·`_-`만 받는다. 운영자 아이디(`X-Admin-Id`)는 조치 API를 만들 때 같은 방식으로 추가한다

### 5.3 폴링 (TanStack Query `useQuery`)
- `initialData`에 서버가 준 첫 데이터를 넣는다
- `refetchInterval`에 함수를 넣어 **최종 상태면 `false`**(멈춤), 아니면 간격(ms)을 돌려준다. 최종 상태 목록은 그 기능의 `types.ts`에 상수로 둔다
- `queryKey`는 `['기능', 식별자]`(예: `['order', orderNo]`)

### 5.4 쓰기 (TanStack Query `useMutation`)
- 버튼은 `isPending` 동안 `disabled`
- 결제처럼 멱등키가 필요한 요청은 키를 **컴포넌트가 처음 그려질 때 한 번** 만든다(`useState(newIdempotencyKey)`). 연타·재시도해도 같은 키가 가서 백엔드가 한 건으로 처리한다(docs/06 §6)
  - `crypto.randomUUID()`를 바로 쓰지 않는 이유: https나 localhost에서만 있어서, EC2를 http로 열면 버튼이 에러를 낸다. `lib/idempotency.ts`가 없을 때 `getRandomValues`로 만든다
- 성공하면 `router.push`(`next/navigation`, `next/router` 아님)로 이동하거나 `router.refresh()`로 서버 데이터를 다시 받는다

## 6. 시각 표시

1. 백엔드는 UTC ISO 문자열(`2026-05-08T15:20:00Z`)을 준다. **화면에 보일 때만** `lib/time.ts`의 `formatKst`로 바꾼다(CLAUDE.md §3-7)
2. `toLocaleString()`을 시간대 없이 쓰지 않는다. 서버(UTC)와 브라우저(KST)가 다른 값을 그려 하이드레이션 에러가 난다
3. "N초 전"처럼 **지금 시각에 따라 바뀌는 표시**는 Client Component에서 화면이 뜬 뒤(`useEffect`) 계산한다
4. 날짜 입력(어드민 기간 필터)은 KST 날짜 문자열(`2026-05-09`) 그대로 보낸다. UTC 변환은 백엔드가 한다

## 7. URL 상태

- 어드민 필터·페이지는 URL 쿼리에 담는다(`/admin?orderStatus=ISSUED&page=1`). 페이지(Server)가 `searchParams`를 읽어 조회하고, 필터 폼(Client)은 `router.push`로 URL만 바꾼다
- 이유: 새로고침·링크 공유해도 같은 화면이 나온다. **확정된 값은 URL 한 곳에만** 둔다. 폼의 `useState`는 [조회]를 누르기 전 고르는 중인 값(초안)만 든다. URL이 바뀌면(초기화, 뒤로 가기) 폼에 `key`를 줘서 새로 만들어 URL과 맞춘다
- URL은 누구나 고쳐 보낼 수 있다. 잘못된 값이면 백엔드가 400을 주고, 페이지는 `ApiError`를 잡아 이유와 [초기화] 링크를 보여 준다

## 8. 이름 규칙

| 대상 | 규칙 | 예 |
|---|---|---|
| 컴포넌트 파일·이름 | PascalCase, 파일 하나에 export 하나 | `IssueStatusPoller.tsx` |
| 서버 조회 함수 | `get{X}` (없으면 ApiError) | `getMyOrder(orderNo)` |
| 브라우저 함수 | 조회 `fetch{X}`, 요청은 동사 | `fetchMyOrder`, `payOrder` |
| 타입 | 백엔드 dto 이름에서 `Response`를 뺀 것 | `OrderResponse` → `Order` |
| 상태 값 | 백엔드 enum 문자열 그대로, 유니온 타입 | `type OrderStatus = 'PENDING' \| 'PAID' \| ...` |

## 9. 새 화면 체크리스트

- [ ] `page.tsx`에 `'use client'`가 없고, 데이터를 읽으면 `export const dynamic = 'force-dynamic'`이 있다 (§4-1, §5.2-1)
- [ ] 서버 조회는 `features/*/server.ts`에, 첫 줄에 `import 'server-only'`가 있다 (§2)
- [ ] `'use client'`는 상호작용이 있는 가장 작은 컴포넌트에만 있다 (§4-2)
- [ ] 컴포넌트에서 `fetch()`를 직접 부르지 않는다 (§5.2-2)
- [ ] 다른 기능에서는 `types.ts`만 import한다 (§3-2)
- [ ] 폴링은 최종 상태에서 멈춘다 (§5.3)
- [ ] 쓰기 버튼은 요청 중 비활성화되고, 멱등키는 한 번만 만든다 (§5.4)
- [ ] 시각은 `formatKst`로만 표시한다 (§6)
- [ ] `types.ts`가 백엔드 dto와 필드 이름·null 가능성이 같다
- [ ] `npm run build`가 통과한다 (타입 검사 + 서버/클라이언트 경계 검사)

## 10. 한계 (알고 고른 것)

- `types.ts`를 손으로 맞춘다. 백엔드 dto가 바뀌면 같이 고쳐야 하고, 틀려도 빌드가 잡지 못한다(OpenAPI 코드 생성은 도구가 하나 더 늘어서 쓰지 않았다)
- 의존 방향(§3)은 체크리스트와 리뷰로만 지킨다. 빌드가 강제하는 것은 `server-only`(서버 파일 경계)와 `'use client'` 규칙(Next 자체 검사)뿐이다
- 인증이 쿠키에 담긴 아이디 문자열뿐이다(과제 범위 밖)
