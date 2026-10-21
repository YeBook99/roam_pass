# L1. Next.js 기초 — 처음 쓰는 사람을 위한 가이드

> 대상: React를 조금 알거나 전혀 모르고, Next.js는 처음인 사람
> 목표: 이 과제의 화면을 만들 수 있고, 인터뷰에서 **"서버/클라이언트 경계를 어디에 그었나"**(질문 6)에 내 말로 답할 수 있게 된다
> 기준 버전: **Next.js 15, App Router**

---

## 0. 한 줄 비유

> **Server Component는 "주방에서 완성해서 내보내는 요리"**, **Client Component는 "손님 테이블에서 직접 굽는 고기"**.
> 대부분의 음식(데이터 표시)은 주방에서 완성해 내보내고, 손님이 직접 조작해야 하는 것(버튼, 입력, 타이머)만 테이블로 보낸다.

---

## 1. 먼저 React 30초 정리

React는 화면을 **컴포넌트**(함수) 단위로 만든다.

```tsx
// 컴포넌트 = HTML 비슷한 것(JSX)을 반환하는 함수
function PlanCard({ country, price }: { country: string; price: number }) {   // props = 부모가 넘겨주는 값
  return <div>{country} — {price.toLocaleString()}원</div>;
}
```

- **props**: 부모가 자식에게 넘기는 값이다. 읽기만 한다
- **state**(`useState`): 컴포넌트가 기억하는 값이다. 바뀌면 화면이 다시 그려진다
- **hook**: `use`로 시작하는 함수(`useState`, `useEffect` 등). 브라우저에서 상호작용할 때 쓴다

## 2. Next.js는 React에 무엇을 더해 주나

| React만 쓸 때 | Next.js가 해 주는 것 |
|---|---|
| URL과 화면 연결(라우팅)을 직접 설정 | **폴더 구조 = URL** (자동) |
| 모든 코드가 브라우저에서 실행됨 | 컴포넌트를 **서버에서 먼저 실행**해 완성된 HTML을 보낼 수 있음 |
| API 서버 연결 설정을 직접 | `rewrites`로 프록시 설정 |
| 빌드와 최적화 설정을 직접 | `next build` 한 번 |

## 3. 라우터 두 가지: Pages Router vs App Router

| | Pages Router (예전 방식) | **App Router (이 과제)** |
|---|---|---|
| 폴더 | `pages/` | `app/` |
| 기본 컴포넌트 | 브라우저에서 실행 | **서버에서 실행 (Server Component)** |
| 데이터 가져오기 | `getServerSideProps` 같은 특별한 함수 | 컴포넌트 안에서 그냥 `await fetch()` |
| 지금 공식 문서 기준 | 유지보수 | **기본 권장** |

App Router를 고른 이유는 ADR-01에 있다.

## 4. App Router의 폴더 규칙 — 폴더가 곧 URL

```
app/
├─ layout.tsx                  → 모든 페이지를 감싸는 틀 (공통 헤더, Provider)
├─ page.tsx                    → URL "/"  (요금제 목록)
├─ checkout/
│  └─ [orderNo]/               → [대괄호] = URL의 변수 부분
│     └─ page.tsx              → URL "/checkout/R20261019-0001"
├─ orders/[orderNo]/waiting/
│  └─ page.tsx                 → URL "/orders/R2026.../waiting"
├─ my-sims/
│  ├─ page.tsx                 → "/my-sims"
│  └─ [orderNo]/page.tsx       → "/my-sims/R2026..."
└─ admin/
   ├─ page.tsx                 → "/admin"
   ├─ orders/[orderNo]/page.tsx
   └─ anomalies/page.tsx
```

특별한 파일 이름

| 파일 | 역할 |
|---|---|
| `page.tsx` | 그 URL의 화면. **이 파일이 있어야 URL이 생긴다** |
| `layout.tsx` | 하위 페이지를 감싸는 틀. 페이지를 이동해도 다시 그려지지 않는다 |
| `loading.tsx` | 서버에서 데이터를 가져오는 동안 보여 줄 화면 |
| `error.tsx` | 에러가 났을 때 보여 줄 화면 (반드시 Client Component) |

`[orderNo]` 값을 꺼내는 법 (**Next 15부터 `params`가 Promise라서 `await`가 필요**):
```tsx
export default async function Page({ params }: { params: Promise<{ orderNo: string }> }) {
  const { orderNo } = await params;
  ...
}
```

## 5. Server Component — 서버에서 실행되는 컴포넌트

**App Router에서 아무 표시도 안 한 컴포넌트는 전부 Server Component다.**

```tsx
// app/my-sims/[orderNo]/page.tsx  (Server Component — 'use client' 없음)
import { cookies } from 'next/headers';

export const dynamic = 'force-dynamic';          // 매 요청마다 새로 그림 (캐시 안 함)

export default async function MySimDetailPage({ params }: { params: Promise<{ orderNo: string }> }) {
  const { orderNo } = await params;
  const userId = (await cookies()).get('userId')?.value;          // 서버에서 쿠키 읽기

  const res = await fetch(`${process.env.API_INTERNAL_URL}/api/my/orders/${orderNo}`, {
    headers: { 'X-User-Id': userId ?? '' },
    cache: 'no-store',                             // 결제 상태는 절대 캐시하지 않음
  });
  const sim = await res.json();

  return (
    <main>
      <h1>{sim.planName}</h1>
      <p>결제 금액: {sim.amount.toLocaleString()}원</p>
      <p>설치 상태: {sim.installStatus}</p>
      <QrReveal orderNo={orderNo} />               {/* ← 버튼이 있는 부분만 Client Component */}
    </main>
  );
}
```

### Server Component의 특징
| 할 수 있는 것 ✅ | 할 수 없는 것 ❌ |
|---|---|
| `async/await`로 데이터 가져오기 | `useState`, `useEffect` 등 hook |
| 서버 전용 환경변수 읽기(`API_INTERNAL_URL`) | `onClick` 같은 이벤트 처리 |
| 쿠키·헤더 읽기(`cookies()`) | `window`, `localStorage`, `setInterval` |
| 무거운 라이브러리를 써도 브라우저로 안 감 | |

### 어떻게 동작하나
```
1. 브라우저가 /my-sims/R2026... 요청
2. Next.js 서버가 page.tsx를 실행 → Spring API 호출 → 결과로 HTML 완성
3. 완성된 HTML을 브라우저로 전송 → 사용자는 즉시 내용을 봄 (로딩 깜빡임 없음)
4. 이 컴포넌트의 JavaScript 코드는 브라우저로 보내지지 않음
```

## 6. Client Component — 브라우저에서 실행되는 컴포넌트

파일 **맨 첫 줄**에 `'use client'`를 쓴다.

```tsx
// components/QrReveal.tsx
'use client';                                       // ← 이 한 줄이 경계선

import { useState } from 'react';

export function QrReveal({ orderNo }: { orderNo: string }) {
  const [open, setOpen] = useState(false);
  const [qr, setQr] = useState<string | null>(null);

  async function reveal() {
    const res = await fetch(`/api/my/orders/${orderNo}/qr`, { method: 'POST', headers: authHeader() });
    setQr((await res.json()).activationCode);       // 서버는 이 요청으로 QR_VIEWED를 기록
    setOpen(true);
  }

  return open ? <QrImage value={qr!} /> : <button onClick={reveal}>QR 코드 보기</button>;
}
```

### 정확히 무슨 일이 일어나나
1. Client Component도 **처음에는 서버에서 HTML로 한 번 그려진다**(버튼 모양만)
2. 브라우저가 JavaScript를 받아 그 HTML에 **이벤트를 연결**한다. 이 과정을 **하이드레이션(hydration)**이라고 한다. "말라 있던 HTML에 물(동작)을 붓는다"는 뜻이다
3. 이후 클릭, 상태 변경, 타이머는 **브라우저에서** 동작한다

## 7. 두 컴포넌트를 섞는 규칙

```
Server Component (page.tsx)
 ├─ 데이터 조회
 ├─ <h1>, <table> 같은 정적 표시
 └─ <ClientComponent initial={data} />   ← 서버가 가져온 데이터를 props로 넘김
```

| 규칙 | 설명 |
|---|---|
| 서버 → 클라이언트로 props 전달 가능 | 단, **직렬화 가능한 값만** 넘길 수 있다(문자열, 숫자, 배열, 일반 객체). 함수, `Date` 객체, 클래스 인스턴스는 안 된다 → 시간은 ISO 문자열로 넘긴다 |
| 클라이언트에서 서버 컴포넌트를 import ❌ | `'use client'` 파일이 import하는 것은 모두 클라이언트가 된다 |
| 클라이언트가 서버 컴포넌트를 `children`으로 받기 ⭕ | 레이아웃 패턴에서 사용 |
| `'use client'`는 **경계에 한 번만** | 그 파일이 import하는 하위 컴포넌트는 자동으로 클라이언트가 된다 |

**경계를 최대한 아래로(잎사귀 쪽으로) 내린다.** 페이지 전체를 `'use client'`로 만들면 Server Component의 장점(빠른 첫 화면, 적은 JS)이 사라진다. 버튼 하나만 클라이언트로 만든다.

## 8. fetch와 캐시

- Next.js는 서버의 `fetch` 결과를 **캐시**할 수 있다. 같은 요청이면 저장된 결과를 재사용한다
- 버전마다 기본값이 바뀌어 왔다(14는 기본 캐시, 15는 기본 캐시 안 함). 헷갈리지 않게 **이 과제는 전부 명시**한다
  - `fetch(url, { cache: 'no-store' })`
  - 페이지 파일에 `export const dynamic = 'force-dynamic'`
- **이유**: 결제 상태가 캐시되어 "발급 완료"인데 "결제 대기"로 보이면 사고다

## 9. 폴링 — TanStack Query로 "주기적으로 다시 묻기"

**폴링(polling)**은 서버에 일정 간격으로 "바뀌었어?"라고 계속 물어보는 방식이다. 발급은 몇 초 뒤 콜백으로 완료되므로 대기 화면이 스스로 다시 물어봐야 한다.

### 9.1 준비: Provider (한 번만)
```tsx
// app/providers.tsx
'use client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';

export function Providers({ children }: { children: React.ReactNode }) {
  const [client] = useState(() => new QueryClient());
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

// app/layout.tsx (Server Component)
import { Providers } from './providers';
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return <html lang="ko"><body><Providers>{children}</Providers></body></html>;
}
```
> layout은 Server Component이지만, Client Component인 `Providers`가 `children`으로 페이지를 감싸는 것은 허용된다(§7의 children 패턴).

### 9.2 발급 대기 폴링
```tsx
'use client';
import { useQuery } from '@tanstack/react-query';

const FINAL = ['ISSUED', 'PICKUP_READY', 'ISSUE_FAILED', 'REFUNDING', 'REFUNDED', 'REFUND_FAILED'];

export function IssueStatusPoller({ orderNo, initial }: { orderNo: string; initial: Order }) {
  const { data } = useQuery({
    queryKey: ['order', orderNo],                  // 캐시 이름표
    queryFn: () => fetch(`/api/my/orders/${orderNo}`, { headers: authHeader() }).then(r => r.json()),
    initialData: initial,                          // 서버가 준 첫 데이터로 시작 → 깜빡임 없음
    refetchInterval: (query) =>
      FINAL.includes(query.state.data?.status) ? false : 2000,   // 최종 상태면 멈춤, 아니면 2초마다
  });
  return <StatusView order={data} />;
}
```

`setInterval`로 직접 짤 때와 비교하면 TanStack Query가 대신 해 주는 일은 이렇다.
- 화면을 떠나면(언마운트) 자동으로 멈춘다 → 화면을 나가도 요청이 계속 나가는 문제가 없다
- 이전 요청이 아직 안 끝났으면 중복으로 보내지 않는다
- 에러가 나면 자동으로 재시도한다
- 브라우저 탭이 백그라운드면 폴링을 멈춘다(기본 동작)

### 9.3 쓰기 요청(결제 버튼): useMutation + 멱등키
```tsx
'use client';
import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';   // App Router용 (next/router 아님!)
import { useState } from 'react';
// authHeader(): 쿠키의 userId를 꺼내 { 'X-User-Id': ... }를 만드는 작은 도우미 함수 (lib/auth.ts)

export function PayButton({ orderNo }: { orderNo: string }) {
  // 페이지에 들어올 때 한 번만 생성 → 연타·재시도해도 같은 키 (06 §6)
  // 실제 코드는 lib/idempotency.ts의 newIdempotencyKey를 쓴다. crypto.randomUUID()는 https·localhost에서만 있어서다
  const [idempotencyKey] = useState(() => crypto.randomUUID());
  const router = useRouter();

  const pay = useMutation({
    mutationFn: () => fetch(`/api/orders/${orderNo}/payments`, {
      method: 'POST',
      headers: { ...authHeader(), 'Idempotency-Key': idempotencyKey },
    }).then(r => r.json()),
    onSuccess: () => router.push(`/orders/${orderNo}/waiting`),
  });

  return <button disabled={pay.isPending} onClick={() => pay.mutate()}>
    {pay.isPending ? '결제 중...' : '결제하기'}
  </button>;
}
```

## 10. 백엔드 연결: rewrites

```ts
// next.config.ts
const nextConfig = {
  async rewrites() {
    return [{ source: '/api/:path*', destination: `${process.env.API_INTERNAL_URL}/api/:path*` }];
  },
};
export default nextConfig;
```
- 브라우저는 `/api/...`(같은 도메인)로 요청하고, Next 서버가 Spring으로 전달한다 → **CORS 설정이 필요 없다**
- Server Component는 `API_INTERNAL_URL`로 Spring을 직접 호출한다

## 11. 환경변수 주의

| 이름 | 어디서 보이나 |
|---|---|
| `API_INTERNAL_URL` | **서버에서만**(Server Component, next.config) |
| `NEXT_PUBLIC_...` | **브라우저에도 노출됨**. 빌드 결과물에 그대로 박힌다 |

→ **비밀 값에는 절대 `NEXT_PUBLIC_`을 붙이지 않는다.**

## 12. 이 과제 화면별 서버/클라이언트 정리

`08_프론트_경계.md` §2 표를 본다. 핵심만 다시 정리하면 이렇다.
- **페이지(`page.tsx`) = Server Component**: 데이터 조회와 표시
- **Client Component**: `PayButton`, `IssueStatusPoller`, `QrReveal`, `RefundButton`, `IccidForm`, `AdminFilters`, `FreshnessBadge`, `AdminActionPanel`

## 13. 자주 하는 실수와 에러 메시지

| 증상 / 에러 | 원인 | 해결 |
|---|---|---|
| `You're importing a component that needs useState. This React hook only works in a client component...` | Server Component에서 `useState` 사용 | 그 부분을 분리해 `'use client'` 파일로 |
| `Event handlers cannot be passed to Client Component props` | 서버에서 `onClick={...}` 함수를 넘김 | 이벤트 처리를 클라이언트 컴포넌트 안으로 |
| `window is not defined` | 서버에서 실행되는 코드가 `window` 접근 | 클라이언트 컴포넌트의 `useEffect` 안에서 |
| `Hydration failed` / 텍스트 불일치 경고 | 서버와 브라우저가 그린 결과가 다름(예: "12초 전"을 서버 시각과 브라우저 시각으로 각각 계산) | 시간 의존 표시는 클라이언트에서 마운트 후 계산 |
| 상태가 안 바뀜(옛날 데이터) | fetch 캐시 | `cache: 'no-store'` + `dynamic = 'force-dynamic'` |
| `params.orderNo`가 undefined | Next 15부터 `params`가 Promise | `const { orderNo } = await params` |
| 날짜가 9시간 다르게 보임 | 브라우저/서버 타임존 차이 | `toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' })` |
| 자정이 `24:20`으로 보임 | `ko-KR` 형식에 `hour12: false`를 쓰면 0시를 24시로 쓴다 | `hourCycle: 'h23'` (`lib/time.ts`) |

## 14. 면접에서 말하는 법

> "Next.js는 이번에 처음 써서 공식 튜토리얼부터 봤습니다. App Router에서는 컴포넌트가 기본적으로 서버에서 실행되고, 파일 맨 위에 `'use client'`를 쓴 것만 브라우저에서 실행됩니다.
> 저는 '처음 보여 줄 데이터는 서버, 사용자가 누르거나 시간이 지나며 바뀌는 건 브라우저'라는 기준으로 나눴습니다. 그래서 페이지는 전부 Server Component로 데이터를 가져와 바로 보여 주고, 결제 버튼과 발급 대기 폴링, QR 열기 버튼만 Client Component로 만들었습니다.
> 발급 대기는 TanStack Query로 2초마다 다시 묻고, 발급 완료나 실패 같은 최종 상태가 되면 폴링을 멈추게 했습니다."

### 스스로 점검
- [ ] Server Component에서 `useState`를 쓰면 왜 안 되는지 설명할 수 있다 (서버에는 "클릭 후 다시 그리기"가 없으니까)
- [ ] `'use client'`를 페이지 전체에 붙이면 무엇을 잃는지 말할 수 있다
- [ ] 결제 상태 조회에 캐시를 끈 이유를 말할 수 있다
- [ ] 폴링이 언제 멈추는지 코드에서 가리킬 수 있다
