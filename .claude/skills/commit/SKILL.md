---
name: commit
description: 변경 사항을 기능 단위로 나눠 .gitmessage 템플릿([왜][무엇][검증][관련])을 지키는 커밋으로 만든다. "커밋해줘", "커밋 나눠줘", "기능별 커밋"을 요청받았을 때 사용.
---

# 기능별 커밋 만들기

과제는 커밋 히스토리로 **어떤 순서로 문제를 풀었는지**를 본다(2.5-6). 커밋 하나가 한 문제를 푼 기록이 되게 한다.

## 절차

0. `git branch --show-current`로 현재 브랜치를 확인한다. `main`이나 `develop`이면 커밋하지 말고 `/branch-pr`로 작업 브랜치부터 만든다
1. `git status --short`와 `git diff --stat`으로 변경 범위를 확인한다
2. 변경을 **기능(문제) 단위**로 묶는다. 묶는 기준은 아래 순서로 판단한다
   - 같은 요구사항 번호(2.3-1~6)를 푸는가
   - 따로 되돌려도 빌드가 깨지지 않는가
   - 기능 코드와 그 기능의 테스트는 **같은 커밋**에 넣어도 되고, 테스트가 크면 `test(scope)` 커밋으로 분리해도 된다
   - 설계 문서나 INTERVIEW_GUIDE 갱신은 관련 기능 커밋에 함께 넣는다(코드와 설명이 같이 움직이도록)
3. 묶음마다 `git add <경로들>`을 실행한다. `git add -A`나 `git add .`는 쓰지 않는다. `.env`가 끼어들지 않게 하기 위해서다
4. 아래 형식으로 메시지를 쓰고 `git commit -F -`(heredoc)로 커밋한다

```
<type>(<scope>): <한글 요약 50자 이내>

[왜] 이 변경이 필요한 이유 (요구사항/문제)
[무엇] 핵심 변경 2~4줄
[검증] 실행한 테스트 이름과 결과, 또는 수동 확인 방법
[관련] 2.3-2, 2.4-Q3 처럼 과제 문서 번호

Co-Authored-By: ... (시스템이 요구하면)
```

- type: feat|fix|test|refactor|docs|chore|build
- scope: plan|order|payment|issue|install|admin|mock|front|infra|harness|arch|learn|ci|e2e

5. commit-msg 훅이 거부하면 메시지를 고쳐 **다시 커밋**한다. `--no-verify`, `--amend`, squash는 쓰지 않는다
6. 마지막에 `git log --oneline -n <개수>`로 결과를 보여 준다

## 금지

- 여러 기능을 한 커밋에 섞는 것 (예: 결제 + 어드민 화면)
- `[검증]`에 "확인함"만 쓰는 것 → 무엇을 실행했는지 구체적으로 쓴다
- 시크릿 파일 스테이징
