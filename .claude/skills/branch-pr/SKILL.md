---
name: branch-pr
description: 기능 하나를 시작할 때 작업 브랜치를 만들고, 끝나면 커밋 정리 → push → develop 대상 PR 생성 → 머지 후 CI 결과 확인까지 진행한다. "기능 시작", "브랜치 만들어", "PR 올려줘", "작업 마무리" 요청 시 사용.
---

# 브랜치 → PR 흐름

## 브랜치 전략 (요약)

```
main      ── 배포되는 코드. develop → main PR로만 변경 (릴리스)
  ▲
develop   ── 통합 브랜치. 작업 브랜치의 PR로만 변경. GitHub 기본 브랜치
  ▲ PR (로컬 테스트·시크릿 스캔·리뷰어 통과 후, CI는 머지 후 실행)
feature/payment-webhook-idempotency   ── 기능 하나 = 브랜치 하나
fix/payment-amount-check
docs/interview-q3
```

- 이름: `<type>/<scope>-<짧은-설명>`. 소문자, 숫자, 하이픈만 쓴다. `.githooks/pre-commit`이 검사한다
  - type: `feature | fix | hotfix | refactor | test | docs | chore | ci | release`
  - scope: 커밋 scope와 같다 (`payment`, `issue`, `admin`, `front`, ...)
- **main/develop 직접 커밋·push 금지**: git pre-commit hook과 Claude guard-bash hook이 막는다
- `hotfix/*`만 예외로 main에서 분기한다. 수정 후 main과 develop 양쪽으로 PR을 올린다

## 1. 기능 시작

```bash
git fetch origin 2>/dev/null; git switch develop && git pull --ff-only 2>/dev/null
git switch -c feature/<scope>-<설명> develop
```
- 원격이 없으면 fetch와 pull은 건너뛴다
- 브랜치 하나에는 **요구사항 하나**만 담는다. 범위가 커지면 브랜치를 나눈다

## 2. 작업 중
- 커밋은 `/commit` 스킬로 기능 단위·템플릿 형식을 지킨다
- UI(`frontend/**`)를 수정한다면 **수정 전에** `/ui-screenshot`의 before 캡처를 먼저 한다

## 3. 마무리 → PR

1. 점검
   - `git log --oneline develop..HEAD`: 커밋이 기능 단위인가
   - `git diff --stat develop...HEAD`: 의도하지 않은 파일이 섞이지 않았나
   - 백엔드 변경 → `/verify`, UI 변경 → `/ui-screenshot` after 캡처
   - **`backend/**`나 `frontend/**`가 바뀌었으면 Agent 도구로 `invariant-reviewer` 서브에이전트를 호출한다**
     - 프롬프트: `기준 브랜치 develop. 이 브랜치의 변경을 검토해 줘.`
     - "위반"은 수정 커밋을 만든 뒤 다시 호출한다. "의심"은 직접 확인한 뒤 수정하거나, 의도한 것이라면 PR 본문 "설계 근거"에 이유를 적는다
     - 리뷰어는 작성 맥락이 없는 독립된 시각이 목적이므로, 호출할 때 "이건 의도한 거야" 같은 변명을 프롬프트에 넣지 않는다
2. push (작업 브랜치만): `git push -u origin <브랜치>`
3. PR 생성 (`gh` CLI가 있으면 사용, 없으면 웹에서 생성)
   ```bash
   gh pr create --base develop --head <브랜치> \
     --title "<type>(<scope>): <요약>" --body-file .mr-assets/<브랜치>/PR_BODY.md
   ```
   - 본문은 `.github/pull_request_template.md` 형식으로 채운다
   - UI 변경이면 `.mr-assets/<브랜치>/SCREENSHOTS.md` 표를 넣고, 이미지는 사용자가 PR 웹 화면에 드래그해서 첨부해야 한다고 안내한다
4. **CI는 PR 단계에서 돌지 않는다.** 머지(develop push) 후에 자동으로 돈다. 그래서 PR 전에 로컬에서 `./gradlew test`와 `git add` 후 시크릿 스캔을 반드시 한다
5. 사용자에게 PR 링크와 남은 할 일(스크린샷 첨부, 리뷰)을 보고한다
6. 머지 후 Actions → **CI**에서 develop push 실행 결과를 확인한다. 실패하면 develop에서 `fix/...` 브랜치를 만들어 바로 고친다

## 4. 머지 후
```bash
git switch develop && git pull --ff-only
git branch -d <브랜치>
```
- 머지 방식은 **Merge commit**을 쓴다(squash 금지). 과제가 작업 흐름이 보이는 커밋 히스토리를 요구하기 때문이다

## 하지 않는 것
- develop/main에 직접 push하거나 로컬에서 merge한 뒤 push하기
- `ALLOW_PROTECTED_COMMIT=1` 사용 (사용자만 예외적으로 쓸 수 있음)
- PR 하나에 여러 기능 섞기
