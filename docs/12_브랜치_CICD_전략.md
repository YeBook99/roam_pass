# 12. 브랜치 전략과 CI/CD (GitHub Actions)

> 이 문서는 **결정과 근거**, 그리고 **GitHub 웹에서 사용자가 직접 해야 하는 설정**을 담는다.
> 작업 절차는 `/branch-pr` 스킬, 워크플로 파일은 `.github/workflows/`에 있다.

## 1. 브랜치 전략

```
main      ●──────────────────────●──────────  배포 (CD 자동 실행)
           \                    ↑ PR (릴리스)
develop     ●────●─────●────────●──────────  통합 · GitHub 기본 브랜치
                 ↑ PR  ↑ PR
feature/...  ●──●      │
fix/...           ●───●
```

| 브랜치 | 역할 | 변경 방법 |
|---|---|---|
| `main` | 배포되는 코드 | develop → main PR만 |
| `develop` | 기능이 모이는 통합 브랜치 (**기본 브랜치**) | 작업 브랜치 PR만 |
| `<type>/<scope>-<설명>` | 기능 하나 | 자유롭게 커밋 |
| `hotfix/*` | 배포 중 긴급 수정 | main에서 분기 → main, develop 양쪽 PR |

**왜 이 전략인가 (1년차 말투)**: "과제가 커밋 히스토리로 '어떤 순서로 문제를 풀었는지'를 본다고 해서, 기능 하나를 브랜치 하나에 담고 PR로 합쳤습니다. Merge commit으로 합치면 develop 이력에서 '결제 웹훅 멱등 처리'라는 묶음과 그 안의 커밋들이 함께 보입니다. 혼자 하는 과제라 git-flow의 release 브랜치까지는 쓰지 않고 main/develop/작업 브랜치 세 단계만 썼습니다."

### 강제 장치
| 장치 | 막는 것 |
|---|---|
| `.githooks/pre-commit` | main/develop 직접 커밋, 규칙에 맞지 않는 브랜치 이름 |
| `.githooks/commit-msg` | 템플릿([왜][무엇][검증][관련])을 지키지 않은 커밋 |
| Claude hook `guard-bash` | Claude의 main/develop push, force push, squash, `--no-verify` |
| GitHub branch protection (§4) | PR 없는 머지, CI 미통과 머지, force push |

## 2. CI/CD 전체 흐름

| 워크플로 | 언제 | 하는 일 |
|---|---|---|
| `pr-guide.yml` | PR 열림·새 커밋 push (자동) | `manual-ci` 상태를 **pending**으로 표시하고 수동 실행 방법을 안내한다. 테스트는 돌리지 않는다 |
| `ci.yml` | **사용자가 웹 UI에서 수동 실행** / develop·main push | 규칙 검사 → 백엔드 테스트 → 프론트 빌드 → e2e → `manual-ci` 상태와 PR 코멘트로 결과 보고 |
| `cd.yml` | main push (릴리스 PR 머지) | 이미지 빌드 → GHCR push → (승인 후) EC2 배포 → 헬스 체크 |

### CI job 구성
```
harness (브랜치 이름 · 커밋 템플릿 · 시크릿 검사 · 구성요소 감지)
   ├─▶ backend  (./gradlew test — Testcontainers로 진짜 PostgreSQL)
   ├─▶ frontend (npm ci · lint · build — 서버/클라이언트 경계 오류 검출)
   └─▶ e2e      (docker compose로 전체 기동 → Playwright headless 핵심 시나리오)
report (manual-ci 커밋 상태 + PR 코멘트)
```
- **아직 코드가 없어도 실패하지 않는다.** `detect-parts.sh`가 `backend/build.gradle`, `frontend/package.json`, `e2e/package.json`이 있는지 보고, 없는 부분의 job은 건너뛴다. 코드가 추가되는 순간 자동으로 그 job이 켜진다

### 왜 PR CI를 수동 실행으로 했나
- 요구사항이다
- 장점: 작업 중에 push할 때마다 CI가 도는 낭비가 없다(private 저장소는 Actions 무료 시간에 한도가 있음). 준비가 됐을 때 한 번 돌린다
- 위험: 실행을 잊고 머지하는 것 → **`manual-ci` 상태를 필수 체크로 걸어서** 막는다. 새 커밋을 push하면 다시 pending이 되므로 **최신 커밋 기준으로** 통과해야 머지할 수 있다

## 3. 러너 — GitLab 러너와의 대응

| GitLab | GitHub Actions |
|---|---|
| GitLab.com 공유(shared) 러너 | **GitHub-hosted runner** (`ubuntu-latest`). 등록 없이 바로 사용 |
| 직접 설치한 `gitlab-runner` | **self-hosted runner** |
| `tags: [docker]` | `runs-on: [self-hosted, linux, roampass]` (라벨) |
| `.gitlab-ci.yml` | `.github/workflows/*.yml` |
| MR / Merge Request | PR / Pull Request |
| `when: manual` / Run pipeline | `workflow_dispatch` / Run workflow |
| CI/CD Variables | Settings → Secrets and variables → Actions |

**이 저장소의 러너 전환 방식**: 모든 job이 `runs-on: ${{ fromJSON(vars.RUNNER_LABELS || '"ubuntu-latest"') }}`를 쓴다.
- 변수가 없으면 GitHub 기본 러너로 **지금 바로** 동작한다
- self-hosted 러너를 붙인 뒤 저장소 변수 `RUNNER_LABELS = ["self-hosted","linux","roampass"]`를 설정하면 워크플로를 고치지 않고 전환된다

### self-hosted runner 붙이는 법 (나중에)
1. Settings → Actions → Runners → **New self-hosted runner** → Linux x64
2. 화면에 나온 명령을 서버에서 실행한다(`./config.sh --url ... --token ... --labels roampass`, `sudo ./svc.sh install && sudo ./svc.sh start`)
3. 서버에 **Docker**(Testcontainers·e2e용)와 Playwright 브라우저 의존성(`npx playwright install-deps`, sudo 필요)을 준비한다
4. 저장소 변수 `RUNNER_LABELS`를 설정한다
5. ⚠️ **public 저장소에는 self-hosted 러너를 붙이지 않는다.** 외부 PR이 내 서버에서 임의 코드를 실행할 수 있다
6. 1GB 프리티어 EC2는 빌드와 테스트를 돌리기에 부족하다. 배포 서버와 러너는 분리하는 것을 권장한다

## 4. GitHub에서 사용자가 직접 할 설정 (체크리스트)

### 4.1 저장소 만들고 push
```bash
git remote add origin https://github.com/<계정>/<저장소>.git
git push -u origin main develop                 # 보호 브랜치 최초 push는 사용자가 직접 (Claude hook이 차단함)
git push -u origin chore/harness-branch-cicd-e2e
```

### 4.2 Settings → General
- [ ] **Default branch → `develop`**
  - ⚠️ **중요**: "Run workflow" 버튼은 **기본 브랜치에 있는 워크플로만** 보여 준다. 기본 브랜치가 main이고 ci.yml이 아직 develop에만 있으면 버튼이 안 보인다
- [ ] Pull Requests: **Allow merge commits만 체크**, Squash merging·Rebase merging 해제 (작업 흐름 보존)
- [ ] Automatically delete head branches 체크

### 4.3 Settings → Branches (또는 Rules → Rulesets) — `develop`, `main` 각각
- [ ] Require a pull request before merging (혼자라면 승인 수 0)
- [ ] **Require status checks to pass → `manual-ci` 추가**
  - 체크 이름은 최근 1주일 안에 한 번 이상 보고된 것만 검색된다 → **먼저 CI를 한 번 수동 실행**한 뒤 추가한다
- [ ] Block force pushes / Restrict deletions

### 4.4 Settings → Actions → General
- [ ] Actions permissions: Allow all actions(또는 GitHub·verified 작성자만)
- 워크플로 파일에 필요한 권한(`statuses: write`, `pull-requests: write`, `packages: write`)을 직접 선언했으므로 기본 토큰 권한은 read-only여도 된다

### 4.5 Settings → Secrets and variables → Actions (배포할 때)
| 종류 | 이름 | 값 |
|---|---|---|
| Variable | `RUNNER_LABELS` | (선택) `["self-hosted","linux","roampass"]` |
| Variable | `DEPLOY_ENABLED` | EC2 준비 후 `true` |
| Variable | `SITE_DOMAIN` | 예: `3-35-12-34.sslip.io` |
| Secret | `EC2_HOST` | EC2 퍼블릭 IP |
| Secret | `EC2_SSH_KEY` | `roampass-key.pem` 내용 전체 |

### 4.6 Settings → Environments → `production`
- [ ] Required reviewers에 본인 추가 → main 머지 후 배포 직전에 **승인 버튼**을 눌러야 배포된다(수동 게이트)

### 4.7 EC2 쪽 (L5와 연결)
- `docker-compose.prod.yml`의 이미지 태그를 `${IMAGE_TAG:-latest}`로 둔다. CD가 커밋 SHA 태그로 배포하므로 어떤 버전이 떠 있는지 알 수 있고 롤백도 쉽다
- EC2에서 GHCR 이미지를 pull하려면 `docker login ghcr.io`(read:packages 토큰)를 한 번 해 둔다. 또는 패키지를 public으로 둔다

## 5. 수동 실행 방법 (매 PR마다)

1. PR을 올리면 봇 코멘트와 `manual-ci: pending`이 뜬다
2. Actions → **CI** → **Run workflow**
   - Use workflow from: **PR 브랜치**
   - base_branch: `develop` (릴리스 PR이면 `main`)
   - run_e2e: 기본 체크
3. 끝나면 PR에 결과 표 코멘트가 달리고 `manual-ci`가 ✅/❌로 바뀐다
4. CLI: `gh workflow run ci.yml --ref <브랜치> -f base_branch=develop`

## 6. 하네스 구성 (참고용 목록)

CLAUDE.md에서 옮겨 왔다. 이 목록은 자동으로 동작하거나 Claude에게 자동으로 보이는 것들이라, 매 세션 읽을 필요는 없다.

| 종류 | 위치 | 내용 |
|---|---|---|
| git hook | `.githooks/` | `pre-commit`(보호 브랜치 커밋 차단, 브랜치 이름 검사), `commit-msg`(템플릿 형식 검사) |
| Claude hook | `.claude/settings.json` | `guard-secrets`(시크릿 쓰기 차단), `guard-bash`(force push·squash·`--no-verify`·보호 브랜치 push 차단), `sync-reminder`(문서 동기화 알림) |
| 스킬 | `.claude/skills/` | `branch-pr`, `commit`, `ui-screenshot`, `e2e`, `adr`, `interview-answer`, `mock-interview`, `state-transition`, `verify` |
| 서브에이전트 | `.claude/agents/` | `invariant-reviewer` — PR 전 diff를 읽기 전용으로 검토 |
| MCP | `.mcp.json` | `playwright` — `@playwright/mcp` headless, 화면 캡처용 |

## 7. 서브에이전트를 추가하는 기준

구현은 메인 에이전트 하나가 한다. 사용자가 모든 코드를 설명해야 하므로 도메인별로 구현을 나누지 않는다. 서브에이전트는 아래 이득 중 하나가 분명할 때만 추가한다.

| 이득 | 예 |
|---|---|
| **독립된 시각**: 작성 맥락 없이 검토 | `invariant-reviewer` |
| **컨텍스트 분리**: 긴 로그·대량 탐색을 요약만 받음 | 후보: 앱 구현 후 e2e 실행·분석 |
| **권한 제한**: 읽기 전용 등으로 실수 범위 축소 | |
| **병렬 처리**: 서로 독립된 작업을 동시에 | |

사용자와 대화가 필요한 작업(모의 면접 등)은 서브에이전트가 아니라 스킬로 만든다. 서브에이전트는 사용자와 대화할 수 없기 때문이다.

## 8. 한계와 다음 단계

- PR 코멘트·상태 기록은 같은 저장소 브랜치의 PR만 지원한다(fork PR은 토큰 권한이 읽기 전용)
- e2e는 매번 이미지를 새로 빌드해서 느리다 → 레이어 캐시(`cache-from: type=gha`) 적용 여지가 있다
- CD 롤백은 수동이다(`IMAGE_TAG=<이전 sha>`로 다시 up)
- 면접 연결: 가산점 "자동 배포"(2.8)의 근거 문서다
