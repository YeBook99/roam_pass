---
name: commit
description: ROAMPASS 저장소의 변경을 기능 단위로 나눠 .gitmessage 템플릿(type(scope) 제목 + 왜/무엇을/검증/관련)에 맞춰 커밋한다. "커밋해줘", "커밋 남겨", 기능 하나를 끝냈을 때 사용.
---

# 기능 단위 커밋

과제는 커밋 히스토리로 "어떤 순서로 문제를 풀었는지"를 본다 (2.5-6). squash 금지, 한 커밋 = 한 기능/한 판단.

## 절차

1. `git status --porcelain` 과 `git diff` (스테이징 포함)로 변경 전체를 본다.
2. 변경을 **논리 단위로 묶는다.** 섞여 있으면 나눈다. 순서 예시:
   - 스키마(`feat(db)`) → 기능(`feat(<scope>)`) → 테스트(`test(<scope>)`) → 문서(`docs`)
   - 정합성 코드와 그 테스트는 같은 커밋이거나 바로 다음 커밋
   - 리팩터링과 기능 변경은 절대 섞지 않는다
3. 단위마다 `git add <구체적 경로>` (`git add -A` / `git add .` 지양 — 의도치 않은 파일 방지).
4. 메시지를 스크래치패드 파일에 쓰고 `git commit -F <파일>` (heredoc 안에 git 옵션 문자열을 넣지 않는다).
5. 커밋 후 `git log --oneline -3` 으로 확인. 훅이 거절하면 메시지를 고친다 (우회 옵션 금지).
6. 해당 REQ 가 끝났으면 `docs/PROGRESS.md` 체크도 같은 커밋 또는 직후 `docs(progress)` 커밋에 포함.

## 메시지 형식 (`.gitmessage`, `.githooks/commit-msg` 가 검사)

```
<type>(<scope>): <한국어 제목, 72자 이내, 마침표 없음>

왜:
- 이 변경이 필요한 이유 (요구사항 / 버그 / 설계 판단)

무엇을:
- 동작 단위로 바뀐 내용 (파일 나열 X)

검증:
- 실행한 테스트 이름과 결과 / 수동 확인 방법 (안 했으면 "없음 - 이유")

관련: REQ-2.4, Q3

Co-Authored-By: <시스템이 지정한 트레일러>
```

- type: feat | fix | test | refactor | docs | chore | build | deploy | perf | style
- scope: plan | order | payment | issuance | usim | tracking | refund | admin | mock-pg | mock-supplier | job | web | db | infra | deploy | harness | docs | repo
- feat / fix / refactor / test 는 `왜:` `검증:` 필수.
- `검증:` 에는 **실제로 실행한 것만** 쓴다. 실행하지 않은 테스트를 통과했다고 쓰지 않는다.
- push 는 사용자가 요청할 때만.
