---
name: ui-screenshot
description: UI(frontend/**)를 수정하는 작업에서 Playwright MCP(headless)로 수정 전/후 화면을 같은 데이터·같은 뷰포트로 캡처하고 PR 첨부용 표를 만든다. 화면 수정 시작 전(before)과 PR 올리기 전(after)에 사용.
---

# UI 변경 전/후 캡처 (Playwright MCP, headless)

UI를 수정한 PR에는 **before/after 이미지가 반드시** 있어야 한다(`CLAUDE.md` §5). 리뷰어가 코드를 읽지 않고도 무엇이 바뀌었는지 볼 수 있게 하기 위함이다.

## 0. 준비
- MCP 서버: `.mcp.json`의 `playwright`(`@playwright/mcp@0.0.83 --headless --isolated --output-dir .mr-assets`). 처음 쓰면 Claude Code가 프로젝트 MCP 사용 승인을 물어본다
  - 브라우저가 없다는 오류가 나면 `mcp__playwright__browser_install`을 한 번 호출한다
- 사용하는 도구: `browser_resize`, `browser_navigate`, `browser_wait_for`, `browser_snapshot`, `browser_take_screenshot`, `browser_close` (Claude에서는 `mcp__playwright__` 접두사)
- 출력 위치: `.mr-assets/<브랜치명에서 / → ->/`. gitignore 대상이며 PR 웹 화면에 드래그해서 첨부한다
- **같은 데이터로 찍어야** 비교가 된다. `node scripts/dev/seed-demo.mjs`로 고정 사용자 `demo-user`의 주문 상태를 만든다(이심 ISSUED 1건, 유심 PICKUP_READY 1건)

## 1. 대상 화면 정하기
변경 파일(`git diff --name-only develop...HEAD -- frontend/`)에서 영향받는 URL을 뽑는다.
- `frontend/app/<경로>/page.tsx` → `/<경로>` (`[orderNo]`는 seed가 출력한 주문번호로 대체)
- 공통 컴포넌트를 수정했다면 그 컴포넌트를 쓰는 대표 화면 1~2개
- 뷰포트는 두 개: **desktop 1280×800**, **mobile 390×844**

## 2. Before 캡처 — 수정 전 화면

**가장 좋은 방법**: 코드를 고치기 전에 지금 실행 중인 앱(포트 3000)에서 바로 찍는다.

이미 고친 뒤라면 develop을 별도 폴더로 띄워서 찍는다.
```bash
git worktree add ../usimesim-before develop
cd ../usimesim-before/frontend && npm ci
API_INTERNAL_URL=http://localhost:8080 npx next dev -p 3100     # 백그라운드로 실행, 백엔드는 공유
```
→ before는 `http://localhost:3100`, after는 `http://localhost:3000`

화면마다 × 뷰포트마다 아래를 반복한다.
1. `browser_resize` (1280×800 또는 390×844)
2. `browser_navigate` → 대상 URL (쿠키가 필요하면 먼저 `/`에서 `demo-user` 입력)
3. `browser_wait_for` → 화면의 핵심 텍스트(또는 1~2초). **로딩 스피너 상태를 찍지 않는다**
4. `browser_take_screenshot` → `fullPage: true`, `filename: "<브랜치>/<화면이름>-<desktop|mobile>-before.png"`
   - 화면이름 예: `my-sim-detail`, `admin-anomalies`, `checkout`

## 3. After 캡처 — 수정 후 화면
2단계와 같은 URL, 같은 뷰포트, 같은 데이터로 `...-after.png`를 찍는다.

## 4. PR 첨부용 표 만들기
```bash
node scripts/dev/screenshot-table.mjs <브랜치명>
```
→ `.mr-assets/<브랜치>/SCREENSHOTS.md` 생성. PR 본문의 "화면 변경" 섹션에 붙이고, 이미지 파일을 PR 편집 화면에 드래그해서 각 칸의 자리표시자를 업로드된 이미지 링크로 바꾸도록 사용자에게 안내한다.

## 5. 정리
- `browser_close`
- worktree를 썼다면 `git worktree remove ../usimesim-before`
- 3100 포트 dev 서버 종료

## 체크
- [ ] before/after가 **같은 URL·뷰포트·데이터**인가
- [ ] 의도한 변경 외에 달라진 곳이 없는가(있다면 PR 본문에 설명하거나 고친다)
- [ ] 개인정보·시크릿이 화면에 찍히지 않았는가(테스트 데이터만 사용)
