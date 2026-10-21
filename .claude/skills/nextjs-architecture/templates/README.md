# templates

`ARCHITECTURE.md`의 규칙을 코드로 옮긴 틀이다. 틀과 문서가 다르면 **문서가 기준**이고, 틀을 문서에 맞게 고친다.
모든 파일은 2026-10-21에 Next.js 15.5.28 + `@tanstack/react-query` 5.104.5 + `server-only` 0.0.1로 `npm run lint`·`npm run build`가 통과하고, 실행 중인 백엔드에 붙여 화면·404·rewrites가 동작하는 것을 확인했다.

## init/ — 뼈대 (경로 그대로 `frontend/`에 복사)

| 틀 | 위치 | 책임 |
|---|---|---|
| `next.config.ts` | `frontend/next.config.ts` (create-next-app 것을 덮어씀) | `/api/*`·`/mock/*` rewrites, standalone 출력 |
| `Dockerfile` | `frontend/Dockerfile` | 빌드 → standalone 실행 (rewrites 주소는 build arg) |
| `.dockerignore` | `frontend/.dockerignore` | 로컬 `node_modules`·`.next`·`.env`를 이미지에 넣지 않는다 |
| `lib/api-error.ts` | `frontend/lib/` | ProblemDetail → `ApiError(status, code, detail)` |
| `lib/api-server.ts` | `frontend/lib/` | 서버 조회 `serverGet`, 404 → `orNotFound` (`server-only`) |
| `lib/api-client.ts` | `frontend/lib/` | 브라우저 `clientGet`·`clientSend`, `X-User-Id` 헤더 |
| `lib/idempotency.ts` | `frontend/lib/` | 멱등키 (`crypto.randomUUID` 없을 때 대체) |
| `lib/user.ts`, `lib/time.ts` | `frontend/lib/` | 쿠키 이름, `formatKst` |
| `components/KstTime.tsx` | `frontend/components/` | 시각 하나 KST 표시 (서버·브라우저 공용) |
| `components/FreshnessBadge.tsx` | `frontend/components/` | "N초 전 데이터" (어드민) |
| `app/layout.tsx`, `providers.tsx`, `error.tsx`, `not-found.tsx`, `globals.css` | `frontend/app/` (create-next-app 것을 덮어씀) | 틀, QueryClientProvider, 에러·404 화면 |

## example/ — 기능 하나를 만드는 모양 (주문·결제)

새 기능을 만들 때 이 파일들을 보고 **같은 모양**으로 쓴다. 실제 화면이라 그대로 써도 된다.

| 틀 | 보여 주는 것 |
|---|---|
| `features/order/types.ts` | 백엔드 dto와 1:1 타입, 상태 유니온, 폴링 멈춤 상태 상수 |
| `features/order/server.ts` | `import 'server-only'` + `serverGet` |
| `features/order/client.ts` | 브라우저 조회 함수 |
| `features/order/components/IssueStatusPoller.tsx` | `useQuery` + `initialData` + 최종 상태에서 멈추는 `refetchInterval` |
| `features/payment/types.ts` | 다른 기능은 `types`만 import |
| `features/payment/client.ts`, `components/PayButton.tsx` | `useMutation` + 멱등키 한 번 생성 + 요청 중 비활성화 + `router.push` |
| `app/checkout/[orderNo]/page.tsx` | Server 페이지: `await params`, `orNotFound`, 두 기능 조립, `force-dynamic` |
| `app/orders/[orderNo]/waiting/page.tsx` | 서버 첫 조회 → Client 폴러에 `initial`로 넘김 |
