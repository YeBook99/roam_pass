# CLAUDE.md — ROAMPASS 과제 작업 규칙

ZIVO 채용 과제: 여행자용 eSIM/USIM 판매 서비스 **ROAMPASS**의
"주문 → 모의 PG 결제 → 모의 공급사 발급 → 어드민 확인 → 설치 추적" 한 줄기를 끝까지 구현한다.
**설계 기준서는 `docs/GUIDE.md`**, 요구사항 ID(`REQ-x.y`, `D-x`)도 거기서 온다. 작업 전에 관련 절을 먼저 읽는다.

## 0. 이 과제에서 가장 중요한 것

- 평가 30%가 **"사용자가 자기 코드를 설명할 수 있는가"** 다. 사용자는 1년차 개발자이고 2차 인터뷰에서 이 코드를 직접 설명한다.
  → **설명 못 할 코드는 넣지 않는다.** 영리한 코드보다 읽히는 코드.
- 25%가 결제·발급 정합성이다. 아래 불변 규칙을 어기는 코드는 기능이 동작해도 실패로 본다.
- 화면 수·UI 완성도는 평가하지 않는다. 가산점(2.8)은 필수 6개와 README 답변이 끝난 뒤에만.
- 구현 우선순위: 결제(REQ-2) → 발급(REQ-3) → 어드민(REQ-5) → 설치 추적(REQ-4) → 주문(REQ-1) → 화면(REQ-6). 단 최소 주문 생성은 결제보다 먼저.

## 1. 스택 (과제 지정: Next.js / Spring / 관계형 DB)

| 영역 | 선택 |
|---|---|
| 백엔드 | Spring Boot 3, **Java 21**, Gradle(wrapper, Kotlin DSL), Spring Data JPA + 조건부 UPDATE, Flyway(`flyway-mysql`) |
| DB | **MySQL 8.4** (로컬: docker compose), 문자셋 utf8mb4 |
| 비동기 | DB 작업 테이블(`async_job`, `refund_attempt`, `sim_profile.next_poll_at`) + `@Scheduled` + `FOR UPDATE SKIP LOCKED` |
| 테스트 | JUnit 5, Testcontainers(MySQL), AssertJ |
| 프론트 | Next.js App Router, TypeScript, 서버 컴포넌트 조회 + 클라이언트 폴링, `/api/*` Route Handler 프록시 |
| 배포 | Vercel(frontend) + Render(backend, Docker) + 무료 MySQL 호스팅 |

새 라이브러리를 추가하려면: ① 정말 필요한지 사용자에게 먼저 묻고 ② `docs/DECISIONS.md`에 "왜 / 버린 것"을 기록한다.

## 2. 디렉터리 구조

```
backend/   Spring Boot (패키지: com.roampass.{plan,order,payment,issuance,usim,tracking,refund,admin,job,callback,common,mock.pg,mock.supplier})
frontend/  Next.js (app/ 아래 사용자 화면, app/admin/ 아래 어드민)
docs/      GUIDE(설계) · ANSWERS_DRAFT(질문 답 초안) · PROGRESS(체크리스트) · DECISIONS(결정 기록) · LEARNING_LOG(학습 기록) · interview/(설명 노트)
.githooks/ commit-msg(템플릿 검사) · pre-commit(시크릿 차단)
.claude/   hooks(가드레일) · skills(작업 흐름)
```

## 3. 커맨드

```bash
./scripts/setup-dev.sh                 # clone 후 1회: git 훅·커밋 템플릿 연결
docker compose up -d db                # 로컬 MySQL 8.4
cd backend && ./gradlew test           # 테스트 (Testcontainers → Docker 필요)
cd backend && ./gradlew bootRun        # API :8080
cd frontend && npm install && npm run dev   # 화면 :3000
```
> 스캐폴딩 후 실제 커맨드와 다르면 이 절을 바로 고친다. README의 실행 방법과 항상 같아야 한다.
> 이 PC의 `npm`은 "Bad CPU type" 에러가 난다 → arm64 Node 재설치가 필요하면 사용자에게 요청.

## 4. 불변 규칙 (결제·발급 정합성)

- **I1 외부 호출은 트랜잭션 밖.** PG·공급사 HTTP 호출을 `@Transactional` 메서드 안에서 하지 않는다. "저장 → 커밋 → 외부 호출 → 결과 반영 트랜잭션" 순서.
- **I2 상태 전이는 조건부 UPDATE.** `UPDATE … SET status=:to WHERE id=:id AND status IN (:from)` 의 영향받은 행 수(0/1)로 "내가 처음인지" 판단한다. 엔티티를 읽고 if로 검사한 뒤 save 하는 방식으로 상태를 바꾸지 않는다.
- **I3 모든 상태 전이는 `status_history`에 같은 트랜잭션으로 기록.** actor(`USER`/`SYSTEM`/`ADMIN:{id}`/`SUPPLIER`/`PG`)와 reason 필수.
- **I4 같은 결과는 같은 메서드로.** PG 승인 응답·웹훅·대사 조회는 하나의 승인 반영 메서드, 공급사 콜백·발급 확인 조회는 하나의 발급 반영 메서드, 설치 콜백·상태 조회는 하나의 설치 반영 메서드를 탄다.
- **I5 콜백은 서명 먼저.** 원본 body 바이트로 HMAC-SHA256 → `MessageDigest.isEqual` 비교. 서명 실패도 `callback_log`에 남기고 처리하지 않는다. 중복 판단은 `callback_log.event_key UNIQUE`(상대가 준 ID + 이벤트 종류, 본문 해시 X).
- **I6 금액은 서버가 정한다.** 주문 금액은 요금제에서 복사, 웹훅 금액 ≠ 주문 금액이면 승인 처리 금지.
- **I7 "결제됨" 상태에는 항상 다음 작업이 붙어 있다.** `PAID`로 바꾸는 트랜잭션에서 발급 job을 같이 만든다. 클라이언트 호출에 의존하는 진행 단계는 없다.
- **I8 재시도는 유한하다.** 모든 재시도는 `attempt_count` / `max_attempts` / 백오프(`next_run_at`)를 가지며, 최대 초과 시 `DEAD` / `FAILED_FINAL`로 바꾸고 어드민 이상 건에 보이게 한다.
- **I9 설치 상태는 앞으로만.** `WHERE install_rank < :newRank`. 되돌아가는 신호는 무시하고 이력만 남긴다.
- **I10 시간은 UTC 저장, KST 표시.** `Instant` 사용, `hibernate.jdbc.time_zone=UTC`. `LocalDateTime.now()` 금지. 만료 = `activated_at + days × 24h`.
- **I11 커밋된 Flyway 마이그레이션은 수정하지 않는다.** 바꿀 게 있으면 새 `V{n+1}__*.sql`. (훅이 막음)
- **I12 시크릿은 환경변수로만.** 커밋되는 설정에는 `${ENV_VAR}`만. `.env`는 커밋 금지, `.env.example`만 허용. (훅이 막음)

## 5. 테스트 규칙

- 테스트는 **무엇을 증명하는지** 이름과 assert로 드러낸다. `assertTrue(true)`, 예외만 안 나면 통과하는 테스트 금지.
- 필수: 동시 웹훅 → 결제 1회 / 동시 콜백 → 프로필 1개 / 발급 실패 → 자동 환불 / UTC→KST 만료. 목록은 GUIDE §10.
- 동시성 테스트는 `ExecutorService` + `CountDownLatch`로 동시에 출발시키고, **DB 행 수**로 검증한다. Testcontainers MySQL 사용 (H2 금지).
- 정합성 코드(I1~I10 관련)를 바꾸면 해당 테스트를 같은 커밋 또는 바로 다음 커밋에 둔다.

## 6. 설명 가능성 규칙

- 추상화는 "두 번째로 필요해질 때" 만든다. 제네릭 프레임워크, 커스텀 어노테이션, 리플렉션, AOP 남용 금지.
- 주석은 **"왜"** 만 쓴다. 특히 조건부 UPDATE, 트랜잭션 분리, 재시도 횟수, 폴링 주기처럼 인터뷰에서 물어볼 지점에는 한 줄 이유를 남긴다.
- 결정이 생기면 `docs/DECISIONS.md`에 3줄(결정 / 왜 / 버린 것)로 남긴다. README Q1의 원재료다.
- 사용자가 내 제안을 고치거나 거절하면, 그 내용을 `docs/LEARNING_LOG.md`의 "AI가 틀린 순간" 후보로 적어 둔다 (README Q7 재료). 단 Q7 본문은 사용자가 직접 쓴다.
- 기능 하나를 끝내면 사용자에게 `/explain`으로 설명 연습을 제안한다.

## 7. 작업 흐름 (한 번에 REQ 하나)

1. `docs/PROGRESS.md`에서 다음 REQ 확인 → GUIDE의 해당 절을 읽는다.
2. 테스트부터 (정합성 항목이면 반드시) → 구현 → 테스트 실행 결과 확인.
3. `docs/PROGRESS.md` 체크, 필요하면 `docs/DECISIONS.md` 기록.
4. `/commit` 규칙대로 기능 단위 커밋. 스킬: `/implement-req REQ-x.y`가 이 흐름 전체를 수행한다.

## 8. 커밋 규칙

- 형식은 `.gitmessage` (git 훅 `commit-msg`가 검사): `<type>(<scope>): <한국어 제목>` + 본문 `왜:` `무엇을:` `검증:` `관련:`
- feat / fix / refactor / test는 `왜:`와 `검증:` 필수. 커밋 끝에 `Co-Authored-By` 트레일러.
- **한 커밋 = 한 기능 / 한 판단.** 스키마·기능·테스트가 섞이면 나눈다. squash, force push, `--no-verify` 금지 (작업 흐름이 평가 대상, 훅이 막음).
- push는 사용자가 요청할 때만.

## 9. 하지 말 것

- 가산점 기능을 필수보다 먼저 만들기 / 회원 시스템 만들기 (헤더 `X-User-Id`, `X-Admin-Id`로 충분)
- 모의 PG·공급사 패키지에서 우리 도메인 테이블·서비스를 직접 참조하기 (HTTP로만 대화)
- `docs/ANSWERS_DRAFT.md`의 【본인】 칸을 대신 채우기, 실행하지 않은 테스트 결과를 README에 쓰기
- 깨질 수 있는 코드를 "문제없다"고 설명하기 — 한계는 한계라고 적는다
