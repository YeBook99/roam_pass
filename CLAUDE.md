# ROAMPASS — 유심·이심 판매 과제 (ZIVO 채용 사전 과제)

여행자용 데이터 SIM 판매의 한 줄기 **결제 → 발급 → 어드민 확인 → 설치 추적**을 끝까지 구현한다.
구현자는 **1년차, Next.js는 처음**이고, 제출 후 60분 코드 리뷰 인터뷰에서 모든 코드를 직접 설명해야 한다.

> **설명할 수 없는 코드는 넣지 않는다.** 결정이나 동작이 바뀌면 그 근거(`docs/0x`, `docs/INTERVIEW_GUIDE.md`)를 같은 커밋에서 고친다.

## 1. 우선순위

- 평가는 이해도·설명(30%)과 결제·발급 정합성(25%)이 가장 크다. 화면 수나 UI 완성도보다 이 둘이 먼저다
- 시간이 부족할 때 구현 순서는 **결제 → 발급 → 어드민 → 설치 추적 → 주문 → 사용자 화면**이다. 유심은 이심보다 먼저 포기할 수 있다. 포기한 것은 README에 적는다

## 2. 기술 스택 (근거: `docs/02_기술선택_ADR.md`)

- Backend: Java 17, Spring Boot 3.3, Gradle wrapper, Spring Data JPA(+ 네이티브 조건부 쿼리), Flyway
- Frontend: Next.js 15 App Router, TypeScript, TanStack Query
- DB: PostgreSQL 16 (로컬 docker compose, 배포 AWS RDS) / 배포: AWS EC2
- 모의 PG·공급사: 같은 앱의 `mock.pg`, `mock.supplier` 패키지. 호출은 반드시 **실제 HTTP**(RestClient)
- 테스트: JUnit5 + Testcontainers + Awaitility, e2e는 Playwright

## 3. 아키텍처 불변 규칙

1. **외부 호출(PG·공급사 HTTP)은 DB 트랜잭션 밖에서 한다.** tx1 상태 선점 커밋 → HTTP → tx2 결과 저장 (`docs/06`)
2. **모든 상태 변경은 `StatusTransitionService`를 거친다.** 조건부 UPDATE(`WHERE id=? AND status=?`)로 선점하고, 영향 행이 1일 때만 `status_history`를 남긴다. 엔티티 setter로 상태를 바꾸지 않는다
3. **중복 방지는 DB가 보장한다.** UNIQUE + `ON CONFLICT DO NOTHING` + 조건부 UPDATE. `synchronized`나 메모리 락에 의존하지 않는다 (`docs/05`)
4. **공급사 콜백은 HMAC-SHA256 서명 검증 후에만 처리한다.** 실패한 콜백도 `callback_log`에 남긴다
5. **발급은 서버가 끝까지 책임진다.** 클라이언트 호출에 의존하지 않고 AFTER_COMMIT 이벤트 + 스케줄러 안전망으로 진행한다
6. **재시도에는 상한이 있다.** 발급 3회, 환불 3회. 넘으면 `*_FAILED`로 두고 어드민 이상 건에 노출한다
7. **시각은 UTC(`Instant`, `timestamptz`)로 저장하고 KST는 표시할 때만 쓴다.** 만료 = `activated_at + days × 24h`
8. **설치·개통된 SIM은 환불하지 않는다.** 환불 직전에 공급사 상태를 동기 조회해서 판단한다
9. **시크릿을 커밋하지 않는다.** `.env.example`에는 자리표시자만 둔다
10. **의미 없는 테스트를 쓰지 않는다.** row 수나 상태 같은 관찰 가능한 결과를 검증한다

## 4. 문서 규칙

- 말투는 **1년차 1인칭**("저는 ~ 때문에 ~를 골랐습니다")으로 쓴다. 모르는 것은 모른다고, 한계는 한계라고 쓴다
- 처음 나오는 용어에는 `docs/learn/` 링크나 한 줄 설명을 붙인다
- 사용자 본인 경험(처음 써 본 것, 투입 시간, 막힌 지점)은 지어내지 않고 ✍️ 빈칸으로 둔다

## 5. 작업 흐름

구현은 **메인 에이전트 하나**가 한다(사용자가 모든 코드를 설명해야 하므로 도메인별로 나누지 않는다).

1. **작업 전에 브랜치부터 만든다.** develop에서 `<type>/<scope>-<설명>` 브랜치를 만든다 → `/branch-pr`
2. **UI(`frontend/**`)를 고칠 때는 수정을 시작하기 전에** before 화면을 캡처한다 → `/ui-screenshot`
3. 상태나 전이를 바꾸면 `/state-transition` 체크리스트를 따른다. 기술 결정이 생기면 `/adr`로 남긴다
4. 커밋은 기능 단위로 나눈다 → `/commit`
5. 결제·발급·환불·설치추적·어드민 흐름이나 그 화면을 바꿨으면 관련 e2e를 실행한다 → `/e2e`. testid·API·설정 계약을 바꾸면 e2e 코드도 같은 PR에서 고친다
6. PR 전에 `invariant-reviewer` 서브에이전트로 diff를 검토한다
7. PR은 develop 대상으로 올리고 **Merge commit**으로만 합친다(squash 금지). PR CI는 자동으로 돌지 않으므로 사용자에게 수동 실행(Actions → CI → Run workflow)을 안내한다

## 6. CI 워크플로를 수정할 때 (`docs/12_브랜치_CICD_전략.md`)

- 새 job은 `scripts/ci/detect-parts.sh` 출력으로 조건을 건다. 아직 없는 구성요소 때문에 실패하지 않게 하기 위함이다
- 수정한 뒤 actionlint로 검증한다. hook과 CI가 같은 규칙을 쓰므로 검사 규칙은 `scripts/lib/`에서 고친다

## 7. 명령어

아직 앱 코드가 없다. 구현하면서 **실제로 실행해 확인한 명령만** 여기에 추가한다.
