# 13. 작업 대화 Q&A 정리

> 과제 분석부터 하네스 구성까지 Claude와 나눈 **질문, 답변, 그때 내린 결정**을 시간순으로 정리한 문서다.
> "왜 이렇게 되어 있지?"가 궁금할 때 결정의 출발점을 찾는 용도로 쓴다. 각 결정의 상세 근거는 링크된 문서에 있다.
> 기간: 2026-10-19 ~ 2026-10-20

---

## 한눈에 보기 — 내린 결정

| # | 주제 | 결정 | 상세 문서 |
|---|---|---|---|
| 1 | 진행 방식 | 지금은 **구현하지 않고** 설계 문서·근거·학습 가이드·하네스만 만든다 | `docs/00`~`11` |
| 2 | 백엔드 | Java 17 + Spring Boot 3.3 + **JPA (+ 네이티브 조건부 쿼리)**. MyBatis·JdbcTemplate 없이 | ADR-03, ADR-04 |
| 3 | 모의 PG·공급사 | 같은 Spring 앱 안 별도 패키지, 호출은 실제 HTTP | ADR-09 |
| 4 | 배포 | **AWS 프리티어 (EC2 + RDS)**. 이번엔 문서만 | `docs/10`, `learn/L5` |
| 5 | 문서 수준 | 1년차, **Next.js 처음** 기준. 처음 나오는 용어는 학습 가이드로 설명 | `docs/learn/` |
| 6 | 언어 | 모든 대화·문서는 한국어 | — |
| 7 | 브랜치 | `main` ← `develop` ← `<type>/<scope>-<설명>`. 기능마다 브랜치, develop으로 PR | `docs/12`, `/branch-pr` |
| 8 | CI/CD | **GitHub Actions**. PR CI는 **수동 실행만**, 러너는 기본 러너 + 변수로 self-hosted 전환 | `docs/12` |
| 9 | UI PR | Playwright MCP(headless)로 before/after 캡처 필수 | `/ui-screenshot` |
| 10 | e2e | 핵심 시나리오 E1~E6만 | `/e2e`, `docs/11` §6 |
| 11 | 에이전트 | 구현은 메인 에이전트 하나. 서브에이전트는 **읽기 전용 리뷰어 1개**만. 모의 면접은 스킬로 | CLAUDE.md §5, `docs/12` §7 |

---

## Q1. 과제 PDF를 분석해서 구현하고, 질문 답변 가이드·CLAUDE.md·커밋 템플릿·하네스를 만들어 줘

**요청 원문 (요약)**
> 1) 필수 요구사항 전체 구현 2) 문서가 요구하는 질문에 1년차 언어로 답하는 가이드 3) 요구사항을 지킬 CLAUDE.md 4) 기능별 커밋과 커밋 템플릿 5) 일관된 개발을 위한 하네스(CLAUDE.md, Hooks, Skills)

**답변 / 진행**
- PDF 14쪽을 분석했다. 핵심은 **"완성된 코드가 아니라 이해와 판단 근거"**를 본다는 것이다. 평가 비중은 이해도·설명 30%, 결제·발급 정합성 25%다
- 환경을 확인했다: Java 17, Node 20, Docker, git 2.24, Gradle 없음(wrapper 사용)
- 진행 중 사용자 요청으로 **구현은 보류**하고 문서·하네스 중심으로 바뀌었다(Q4)

**결과물**: `docs/00_요구사항_분석.md`(질문 21개가 어느 문서에서 답해지는지 연결한 커버리지 표), 하네스 일체

---

## Q2. JPA의 네이티브 쿼리는 어때? JPA로는 힘든가?

**답변**
- **JPA만으로 충분하다.** 이 과제에서 까다로운 부분은 "같은 요청이 여러 번 와도 한 번만 처리"다. 이것은 `@Modifying @Query(nativeQuery = true)`로 두 가지 SQL을 직접 쓰면 된다
  - 조건부 UPDATE: `UPDATE ... WHERE id=? AND status='PENDING'` → 바뀐 행이 1이면 처리
  - 중복 무시 INSERT: `INSERT ... ON CONFLICT DO NOTHING`
- 그래서 JdbcTemplate이나 MyBatis를 섞을 필요가 없다
- 면접용 한 줄: "일반 조회·저장은 JPA로 빠르게, 동시성이 중요한 곳만 SQL을 직접 써서 동작이 눈에 보이게 했다"

**같이 정한 것**: 모의 PG·공급사는 같은 앱의 별도 패키지에 두되 실제 HTTP로 호출한다. 배포는 나중에 하고, git은 로컬 커밋만 한다(push는 사용자가 직접)

**관련**: `docs/02` ADR-04, `learn/L3` §3.3

---

## Q3. 제발 한글로 말해

**답변**: 이후 모든 대화와 문서를 한국어로 진행한다.

---

## Q4. 당장 구현할 필요는 없고, 아키텍처 문서와 근거를 1년차 입장에서 상세하게 써 줘. 내가 문제를 푸는 입장이니 잘 설명할 수 있어야 한다

**답변 / 결정**
- 구현을 보류하고 **설계 문서 12개(`docs/00`~`11`) + 답변 대본(`INTERVIEW_GUIDE.md`)**을 만든다
- 대상 질문은 다음과 같다. 모두 `INTERVIEW_GUIDE`에 "30초 답변 → 근거 → 꼬리질문 → 한계 인정 → 자기 점검" 형식으로 정리했다
  - 2.4 필수 8개(Q1~Q8)
  - 2.9 인터뷰 예상 9개(I1~I9)
  - 부록 A 4개(A1~A4)
- **본인 경험은 대신 쓰지 않는다.** Q7(처음 써 본 것), AI가 틀린 순간, 투입 시간, 배포하며 막힌 지점은 ✍️ 빈칸으로 남겼다

---

## Q5. Next.js를 처음 쓰는 사람 기준으로 Server/Client Component 가이드, "상태 선점" 설명, 비동기 어노테이션, ExecutorService·CountDownLatch 설명이 필요해. "설치 추적의 신뢰도 순서"는 무슨 말이지? 배포는 AWS 프리티어로 할 거야

**답변 — 학습 가이드 5개를 추가했다 (`docs/learn/`)**

| 궁금했던 것 | 한 줄 답 | 문서 |
|---|---|---|
| Server Component | 서버에서 실행되어 완성된 HTML로 내려오는 컴포넌트. 데이터를 바로 가져오지만 `useState`·`onClick`은 못 쓴다 | `L1` §5 |
| Client Component | 파일 맨 위 `'use client'`. 브라우저에서 클릭·입력·폴링을 담당한다 | `L1` §6 |
| 상태 선점 | "상태가 A일 때만 B로"를 UPDATE 한 문장으로 DB에 맡기는 것. 바뀐 행이 1이면 내가 처리권을 차지, 0이면 이미 누가 처리 중(의자 뺏기) | `L3` §3 |
| `@Transactional` | 프록시가 메서드 앞뒤로 begin/commit/rollback. **같은 클래스 안에서 부르면 적용 안 됨** | `L2` §2 |
| `@Async` | 다른 스레드에서 실행. 서버가 죽으면 대기 작업이 사라진다 | `L2` §3 |
| `@TransactionalEventListener(AFTER_COMMIT)` | 결제가 **커밋된 뒤에만** 발급 시작 → 롤백된 결제에 발급이 나가지 않는다 | `L2` §4 |
| `@Scheduled` | 주기 실행. `@Async`에서 놓친 일을 DB 상태를 보고 다시 주워 가는 안전망 | `L2` §5 |
| ExecutorService | 스레드풀에 일을 던지는 Java 표준 도구. 여러 스레드가 동시에 실행하게 한다 | `L4` §2 |
| CountDownLatch | 숫자가 0이 될 때까지 기다리는 관문. ready/start/done 3개로 "진짜 동시에" 출발시킨다 | `L4` §3 |
| AWS 프리티어 배포 | EC2 1대(docker compose) + RDS. **2025-07-15 이후 가입 계정은 크레딧 방식 6개월 Free plan**이므로 Billing에서 먼저 확인 | `L5` |

**"설치 추적의 신뢰도 순서"란?**
설치 여부를 알려 주는 정보 출처(신호)가 세 가지이고, 서로 다른 말을 할 때 **무엇을 기준으로 삼을지** 정한 순서다.

| 신호 | 특징 | 쓰는 법 |
|---|---|---|
| QR 열람(우리가 기록) | 확실히 받지만 "봤다"일 뿐 설치 증거가 아님 | 설치 근거로 쓰지 않음, 참고만 |
| 공급사 콜백 | 빠르지만 가끔 빠지거나 늦게 옴 | 오면 바로 반영 |
| 공급사 상태 조회 | 물어볼 때만 알 수 있지만 그 순간의 사실 | 빠진 콜백을 메우는 기준, **환불 직전 필수** |

→ "빠르게 알 때는 콜백, 정확하게 판단할 때는 조회, QR 열람은 근거로 쓰지 않음" (`docs/07` §1)

---

## Q6. 브랜치 전략, CI/CD 파이프라인, UI MR의 Playwright 전후 캡처, 핵심 e2e를 반영해 줘. 크면 스킬로 분리

**요청 원문 (요약)**
> 1) 기능별 브랜치 → develop MR 2) 러너가 추가되면 바로 돌 CI/CD, develop MR은 웹 UI에서 수동 실행 3) UI MR은 Playwright MCP headless로 수정 전/후 캡처 4) e2e는 핵심 시나리오만

**결과**
- 브랜치: `.githooks/pre-commit`이 main/develop 직접 커밋과 잘못된 브랜치 이름을 막는다. Claude hook은 보호 브랜치 push를 막는다 → `/branch-pr` 스킬
- CI/CD: `ci.yml`(수동 실행), `pr-guide.yml`(PR마다 `manual-ci` 대기 상태 + 안내), `cd.yml`(main → GHCR → 승인 후 EC2). actionlint로 검증했다
- UI 캡처: `.mcp.json`(Playwright MCP headless) + `/ui-screenshot` 스킬
- e2e: `e2e/`에 6개 시나리오, 9개 테스트를 썼다. **앱이 아직 없어서 실행은 구현 후에 한다.** 대신 화면 testid·API·설정을 `/e2e` 스킬 §4의 "구현 계약"으로 고정했다

---

## Q7. GitLab이 아니라 GitHub이야. GitHub Actions인가?

**답변**: 맞다. 용어는 이렇게 대응된다.

| GitLab | GitHub |
|---|---|
| MR (Merge Request) | PR (Pull Request) |
| `.gitlab-ci.yml` | `.github/workflows/*.yml` |
| Run pipeline / `when: manual` | **`workflow_dispatch`** (Actions → Run workflow) |
| 보호 브랜치 | Branch protection rules / Rulesets |
| CI/CD Variables | Settings → Secrets and variables → Actions |

---

## Q8. GitLab 러너밖에 모르는데, 대응되는 게 GitHub Actions에 있나?

**답변**: 있다.

| GitLab | GitHub Actions |
|---|---|
| GitLab.com 공유 러너 | **GitHub-hosted runner**(`ubuntu-latest`). **등록 없이 바로 사용** |
| 직접 설치한 `gitlab-runner` | **self-hosted runner** (Settings → Actions → Runners) |
| `tags: [docker]` | `runs-on: [self-hosted, linux, ...]` |

**결정**: 지금은 기본 러너로 바로 돌게 한다. 나중에 self-hosted를 붙이면 **저장소 변수 `RUNNER_LABELS` 하나만** 설정하면 전환된다. PR CI는 **수동 실행만** 한다(선택).

주의: "Run workflow" 버튼은 **기본 브랜치에 있는 워크플로만** 보이므로 GitHub 기본 브랜치를 `develop`으로 설정해야 한다(`docs/12` §4.2)

---

## Q9. e2e나 스킬들은 어떻게 동작해? 메인 에이전트가 호출하나?

**답변**: 종류에 따라 실행 주체가 다르다.

| 종류 | 누가 실행하나 | 강제되나 |
|---|---|---|
| **Skill** (`.claude/skills/*/SKILL.md`) | **메인 에이전트**가 description을 보고 필요하다고 판단할 때 불러와 지침대로 수행한다. 사용자가 `/이름`으로 직접 부를 수도 있다. 서브에이전트가 아니다 | ❌ 에이전트 판단 (놓칠 수 있음) |
| **Claude hook** (`.claude/settings.json`) | Claude Code가 도구 실행 직전·직후에 **자동** 실행 | ✅ (차단형은 우회 불가) |
| **git hook** (`.githooks/`) | git이 커밋할 때 **누가 커밋하든** 실행 | ✅ |
| **CI** (GitHub Actions) | GitHub 서버 | ✅ (branch protection과 연결 시) |
| **e2e 테스트** (`e2e/tests`) | 일반 테스트 코드. 사용자 터미널, CI, 또는 에이전트가 Bash로 실행 | 실행했을 때만 |
| **Playwright MCP** | 에이전트가 브라우저를 직접 조작하는 **도구**(캡처용). e2e와는 별개 | — |

**남은 약점**: UI 캡처(특히 **수정 전** before)와 e2e 실행은 에이전트 판단에 의존한다. 강제하려면 다음 방법이 있다(아직 적용 안 함).
- `frontend/**`를 처음 수정할 때 before 캡처가 없으면 막는 PreToolUse hook
- `frontend/**`가 바뀌었는데 PR 본문의 화면 표가 비면 실패하는 CI 검사

---

## Q10. 지금 에이전트가 메인 하나뿐인데, 에이전트를 구현할 필요성은?

**답변**: **대부분 필요 없다.** 서브에이전트는 아래 이득이 분명할 때만 가치가 있다.
- 독립된 시각(작성자 맥락 없이 검토)
- 컨텍스트 분리(긴 로그를 요약만 받음)
- 권한 제한(읽기 전용 등)
- 병렬 처리

비용도 있다. 매번 맥락을 다시 파악해서 느리고, 사용자와 대화할 수 없다.

| 후보 | 판단 | 이유 |
|---|---|---|
| 도메인별 구현 에이전트 | ❌ | 이해가 조각나 **내가 코드를 설명하기 어려워짐**. 이 과제의 가장 큰 위험 |
| **불변 규칙 리뷰어** | ✅ 도입 | PR 전 diff를 읽기 전용·독립된 눈으로 정합성 규칙 위반 검사 |
| e2e 실행·분석 | ⏸ 보류 | 앱이 생긴 뒤 판단 |
| 모의 면접관 | 스킬로 | 대화형이라 서브에이전트로는 불가 → `/mock-interview` |

**결과**
- `.claude/agents/invariant-reviewer.md`: `/branch-pr` 3단계에서 호출
- `/mock-interview` 스킬
- "서브에이전트를 추가하는 기준"을 정리했다(`docs/12` §7)
- 참고: 새로 만든 에이전트 정의는 **바로 호출되지 않을 수 있다.** 이번에는 파일을 만든 직후 호출이 "not found"였고, 잠시 뒤 세션에 자동으로 등록됐다. 목록에 없으면 잠시 기다리거나 Claude Code를 다시 시작한다

**리뷰어 검증 결과**: 규칙을 일부러 어긴 임시 샘플(`SamplePaymentService`, 커밋하지 않고 삭제함)로 시험했다.
- 넣어 둔 위반 5개를 모두 줄 번호와 함께 찾았다
  - A1: 트랜잭션 안에서 PG 호출
  - A2: setStatus로 상태 변경
  - A3: synchronized와 "조회 후 저장"
  - A6: `while(true)` 무한 재시도
  - A7: `LocalDateTime.now()`
- "의심" 2건도 보고했다. 하나는 환불 전 공급사 조회 누락이고, 다른 하나는 샘플 파일이 커밋될 위험이다
- **실제 누락 1건**도 찾았다: e2e·Playwright MCP를 추가했는데 ADR이 없었다 → `ADR-14`를 추가했다
- 작성자 맥락 없이 보는 리뷰어가 메인 에이전트가 놓친 것을 잡은 사례다. 서브에이전트 도입 근거("독립된 시각")가 실제로 확인됐다

---

## Q11. 지금까지의 질문과 답변을 문서로 정리해 줘

→ 이 문서.

---

## 사용자가 직접 할 일 (모아 보기)

| 할 일 | 위치 |
|---|---|
| ✍️ Q7(처음 써 본 것), AI가 틀린 순간, 투입 시간, 배포하며 막힌 지점 작성 | `INTERVIEW_GUIDE.md` |
| ✍️ 동시성 테스트 "조건을 빼면 실패하는지" 직접 확인 | `learn/L4` §8 |
| GitHub 저장소 생성 후 `git push -u origin main develop <작업 브랜치>` | `docs/12` §4.1 |
| 기본 브랜치 `develop`, Merge commit만 허용, `manual-ci` 필수 체크 | `docs/12` §4.2~4.3 |
| AWS 계정 무료 정책 확인 + Budget 알림 | `learn/L5` §1~2 |
| Claude Code 재시작 → 프로젝트 MCP(playwright) 사용 승인 | — |
| 구현 시작 시 `/branch-pr` → 기능별 브랜치 | — |
| 면접 연습 `/mock-interview` | — |
