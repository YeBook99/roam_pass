---
name: nextjs-architecture
description: Next.js 15 App Router 프론트(frontend/)를 ARCHITECTURE.md 기준으로 만든다. 프론트가 없을 때는 init(create-next-app → 공통 lib·Provider·rewrites·Dockerfile), 화면·기능을 추가할 때는 page(서버/클라이언트 설계안 확인 → 같은 구조로 구현 → 체크리스트 점검). "프론트 시작", "Next.js 뼈대", "화면 추가", "페이지 만들어", "사용자 화면", "어드민 화면" 요청 시 사용.
---

# Skill: nextjs-architecture

## Purpose

모든 화면이 같은 구조(페이지는 Server, 상호작용만 Client, 기능별 `features/*`)를 갖게 한다. 구현자가 Next.js를 처음 쓰므로 **규칙마다 이유를 말할 수 있는 코드**만 만든다. 기준은 같은 폴더의 `ARCHITECTURE.md` 하나이고, 코드 모양은 `templates/`를 따른다.

## 공통 준비 (두 모드 모두)

1. 이 폴더의 `ARCHITECTURE.md`, `templates/README.md`를 읽는다
2. `docs/08_프론트_경계.md` §2(화면별 서버/클라이언트 표)와 `docs/learn/L1_Nextjs_기초.md` §13(자주 하는 실수)을 확인한다
3. 화면이 부를 백엔드 API의 응답 dto(`backend/src/main/java/com/roampass/{기능}/dto/*Response.java`)와 컨트롤러 경로를 읽는다. **`types.ts`는 이 dto를 보고 쓴다.** 추측으로 필드를 만들지 않는다
4. node는 `PATH="/usr/local/bin:$PATH"`로 실행한다(nvm의 node는 이 기기에서 돌지 않는다)

## Procedure — init (뼈대)

`frontend/package.json`이 없을 때 쓴다.

1. 프로젝트 생성 (루트에서)
   ```bash
   npx -y create-next-app@15.5.28 frontend --ts --eslint --app --no-src-dir --no-tailwind --no-turbopack --import-alias "@/*" --use-npm --yes
   cd frontend && npm i @tanstack/react-query@5.104.5 server-only@0.0.1
   ```
2. `app/page.module.css`를 지우고 `templates/init/`를 경로 그대로 `frontend/`에 복사한다(`next.config.ts`, `app/layout.tsx`, `app/globals.css`는 덮어쓴다). `app/page.tsx`는 첫 화면을 만들 때 page 모드로 쓴다
3. `npm run lint && npm run build`를 실행한다
4. docker compose에 `frontend` 서비스를 추가한다. build arg와 실행 환경변수 모두 `API_INTERNAL_URL=http://backend:8080`, 포트 `3000:3000`, `depends_on: backend`. `.env.example`에는 시크릿이 아닌 값만 둔다
5. `docker compose up -d --build --wait` 후 `curl localhost:3000`과 `curl localhost:3000/api/plans`(rewrites)를 확인한다
6. `scripts/ci/detect-parts.sh`가 `frontend=true`를 내는지 확인한다(CI 프론트 빌드 job이 켜진다)
7. 만든 파일 표(파일 / 한 줄 책임)와 실행 결과를 보고한다. CLAUDE.md §7에 **실제로 실행한** 명령만 추가한다

## Procedure — page `<화면 이름>` (화면·기능 추가)

1. 공통 준비를 한다. `frontend/`가 없으면 init부터 하자고 제안한다
2. `frontend/features/`에서 같은 기능 폴더, 재사용할 `types`·`client`·컴포넌트가 있는지 찾는다
3. **UI를 고치는 작업이면** 시작 전에 `/ui-screenshot`으로 before를 찍는다(CLAUDE.md §5-2. 새 화면이면 생략)
4. **코드를 쓰기 전에** 설계안을 보여 주고 확인을 받는다

   ```
   ## 화면 설계안 — <화면 이름> (<URL>)

   | 부분 | 서버/클라이언트 | 파일 | 부르는 API | 이유 |
   |---|---|---|---|---|
   | 페이지 | Server | app/.../page.tsx | GET ... | 첫 화면 데이터 |
   | <버튼·폴러·폼> | Client | features/<기능>/components/Xxx.tsx | POST ... | 클릭/타이머 |

   - 새 타입: features/<기능>/types.ts ← backend <Dto>Response
   - 폴링: 간격 / 멈추는 상태 (없으면 "없음")
   - 쓰기: 멱등키 필요 여부, 성공 후 이동(router.push / router.refresh)
   - URL 상태(searchParams): 있으면 키 목록
   - 다른 기능에서 가져오는 것: types만 (무엇)
   ```
5. 확인을 받으면 **types → server/client 함수 → Client 컴포넌트 → page** 순서로 `templates/example/`과 같은 모양으로 만든다
6. `npm run lint && npm run build`. 실패하면 규칙을 끄지 말고 코드를 고친다
7. 백엔드를 띄우고(`docker compose up -d --wait`) `npm run dev`(또는 compose의 frontend)로 화면을 직접 열어 본다. 최소한 정상 화면, 없는/남의 주문(404), 버튼 연타를 확인하고 결과를 보고한다
8. `ARCHITECTURE.md` §9 체크리스트를 항목마다 (✅/❌, 근거 `파일:줄`) 표로 보고한다
9. 다음으로 넘긴다
   - UI를 바꿨다 → `/ui-screenshot` after
   - 새 라이브러리를 넣었거나 문서와 다르게 결정했다 → `/adr`
   - 커밋 → `/commit`

## Heuristics

- 설계안의 결정마다 `ARCHITECTURE.md §n`을 붙인다. 근거를 댈 수 없으면 다시 본다
- 망설여지면 Server Component로 둔다. 클릭·타이머가 생길 때만 그 조각을 떼어 `'use client'`로 만든다
- 한 페이지에 Client 컴포넌트가 셋을 넘으면 페이지를 나눌 수 있는지 먼저 제안한다
- 화면에 보이는 문구는 `docs/08`(예: 발급 대기 안내 문구)에 있으면 그대로 쓴다

## Do Not

- 설계안을 보여 주지 않고 바로 구현하지 않는다
- `page.tsx`·`layout.tsx`에 `'use client'`를 붙이지 않는다
- 컴포넌트에서 `fetch()`를 직접 부르지 않는다. `lib/api-*`를 거친다
- `ARCHITECTURE.md` §1의 "쓰지 않는 것"(Tailwind, Server Actions, Route Handlers, 전역 상태 라이브러리 등)을 들이지 않는다. 필요하면 먼저 문서와 ADR을 고치고 사용자 확인을 받는다
- `NEXT_PUBLIC_` 환경변수에 주소나 비밀 값을 넣지 않는다
- 실행하지 않은 빌드·화면 확인을 통과로 보고하지 않는다
