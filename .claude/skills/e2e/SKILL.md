---
name: e2e
description: 핵심 시나리오 e2e(Playwright, headless)를 실행·추가·수정한다. 결제·발급·환불·설치추적·어드민 흐름이나 그 화면을 바꿨을 때, "e2e 돌려줘", "시나리오 테스트" 요청 시 사용. data-testid·API·설정 계약을 함께 관리한다.
---

# e2e 핵심 시나리오

**원칙: e2e는 "돈과 SIM이 걸린 한 줄기"만 다룬다.** 세부 규칙(동시성, 경계값)은 백엔드 테스트가 맡고, e2e는 "화면부터 DB까지 실제로 이어지는가"를 본다. 시나리오를 늘리기 전에 백엔드 테스트로 충분하지 않은지 먼저 따진다.

## 1. 시나리오 목록 (`e2e/tests/`)

| ID | 파일 | 검증 | 과제 근거 |
|---|---|---|---|
| E1 | `e1-esim-purchase` | 요금제→결제→대기→ISSUED→QR_VIEWED / **브라우저를 닫아도 발급 완료** | 2.1, 2.3-3, Q2 |
| E2 | `e2-payment-idempotency` | 버튼 연타·웹훅 3중복에도 결제 1번, 전이 1번 | 2.3-2, Q3, Q4 |
| E3 | `e3-issue-failure-refund` | 발급 실패·콜백 누락 → 재시도 상한 → 자동 환불, 화면에 실패 안내 | 2.3-3, Q2, I4 |
| E4 | `e4-usim-iccid` | 자릿수·체크디지트·중복 ICCID 거절, 올바른 ICCID로 개통 | 2.3-3 |
| E5 | `e5-admin-anomaly-refund` | REFUND_FAILED가 이상 건에 노출 → 사유 입력 수동 환불 → 타임라인에 운영자 | 2.3-5, I7 |
| E6 | `e6-install-tracking` | 콜백 누락 설치를 재확인으로 반영 → 환불 불가 → 개통 시 KST 만료일 | 2.3-4, Q5, I6 |

새 시나리오를 추가하는 기준: **위 6개로 덮이지 않는 "사용자가 돈을 내거나 돌려받는 경로"가 생겼을 때만** 추가한다. 추가하면 이 표와 `docs/11_테스트_전략.md`를 함께 고친다.

## 2. 실행

```bash
# 1) 전체 스택을 e2e 프로필로 기동 (짧은 타임아웃, 모의 시스템 활성)
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build --wait

# 2) 처음 한 번
cd e2e && npm ci && npx playwright install chromium

# 3) 실행 (headless 기본)
npx playwright test                    # 전체
npx playwright test e3                 # 파일 이름 일부로 선택
npx playwright test --headed           # 눈으로 보며 디버깅할 때만
npx playwright show-report             # 실패 시 trace/video 확인

# 4) 정리
docker compose -f docker-compose.yml -f docker-compose.e2e.yml down -v
```
- CI에서는 `ci.yml`의 e2e job이 같은 절차로 돈다(수동 실행 시 run_e2e 체크)
- 환경변수: `E2E_BASE_URL`(기본 http://localhost:3000), `E2E_API_URL`(기본 http://localhost:8080)

## 3. 작성 규칙

- **셀렉터는 `data-testid`만** 쓴다. 문구나 CSS 클래스는 바뀌기 쉽다
- 상태는 텍스트가 아니라 **`data-status` 속성**으로 검증한다(문구 번역·변경에 영향받지 않게)
- 비동기 결과는 `expect.poll` / `toHaveAttribute(..., { timeout })`으로 기다린다. `waitForTimeout`(고정 sleep)은 금지
- 모든 describe의 `beforeEach`에서 `resetMocks()`를 호출한다. 모의 설정은 서버 전역이므로 **workers=1, 직렬 실행**이다
- 사용자 아이디는 `uniqueUser()`로 매번 새로 만든다(이전 실행 데이터와 섞이지 않게)
- 검증은 화면 + **어드민 API로 DB 결과**까지 확인한다(예: 승인 결제 1건, 전이 이력 1줄)

## 4. 구현 계약 — 화면·API·설정이 반드시 지켜야 하는 것

e2e가 먼저 작성되어 있으므로, **프론트·백엔드를 구현할 때 아래 계약을 맞춘다.** 계약을 바꾸면 e2e 코드도 같은 PR에서 고친다.

### 4.1 data-testid
| 화면 | testid | 비고 |
|---|---|---|
| 공통 | `user-id-input`, `user-id-submit` | 쿠키 이름 `userId` |
| `/` | `plan-card-{planCode}` + 속성 `data-sim-type="ESIM\|USIM"`, 카드 안 `plan-select-button` | |
| `/` 또는 주문 폼 | `travel-start-date-input`(YYYY-MM-DD), `create-order-button` | 생성 후 `/checkout/{orderNo}`로 이동 |
| `/checkout/{orderNo}` | `order-status`(`data-status`), `pay-button` | 결제 요청 후 `/orders/{orderNo}/waiting`으로 이동 |
| 발급 대기 | `order-status`(`data-status`), `issue-failed-notice`(실패·환불 시 표시) | |
| `/my-sims/{orderNo}` | `order-status`, `install-status`(`data-status`), `refund-eligibility`(`data-refundable="true\|false"`), `qr-reveal-button`, `activation-code`, `recheck-button`, `expires-at`(KST 표기 포함) | |
| 유심 | `iccid-input`, `iccid-submit`, `iccid-error` | |
| 어드민 공통 | `admin-id-input`, `admin-id-submit`, `admin-freshness`("N초 전") | |
| `/admin/anomalies` | `anomaly-row-{orderNo}`(`data-type`) | |
| `/admin/orders/{orderNo}` | `admin-refund-button`, `admin-retry-issue-button`, `admin-reason-input`, `admin-action-submit`, `timeline-item` | |

### 4.2 API (e2e helpers가 호출)
| 메서드 | 경로 | 용도 |
|---|---|---|
| POST | `/mock/reset` | 모의 PG·공급사 설정 초기화 |
| PUT | `/mock/supplier/config` | `issueFailRate`, `callbackDelayMs`, `callbackDuplicates`, `callbackDropRate`, `shuffleOrder` |
| PUT | `/mock/pg/config` | `webhookCount`, `webhookDelayMs`, `cancelFailRate` |
| POST | `/mock/supplier/profiles/{iccid}/state` | `{ state, sendCallback }` 설치·개통 흉내 |
| GET | `/mock/pg/payments?orderNo=` | 모의 PG가 받은 승인 요청 목록 |
| GET | `/api/admin/orders/{orderNo}` (`X-Admin-Id`) | `{ orderNo, status, payments[], profile, timeline[{target,from,to,actor,signal,at}] }` |

### 4.3 설정 (docker-compose.e2e.yml → Spring relaxed binding)
| 환경변수 | 프로퍼티 | 운영 기본 | e2e |
|---|---|---|---|
| `APP_ORDER_PAYMENT_TTL` | `app.order.payment-ttl` | PT10M | PT2M |
| `APP_ISSUE_CALLBACK_TIMEOUT` | `app.issue.callback-timeout` | PT2M | PT5S |
| `APP_ISSUE_MAX_ATTEMPTS` | `app.issue.max-attempts` | 3 | 3 |
| `APP_REFUND_MAX_ATTEMPTS` | `app.refund.max-attempts` | 3 | 3 |
| `APP_REFUND_BACKOFF` | `app.refund.backoff` | PT1M,PT5M | PT1S,PT2S |
| `APP_SCHEDULER_DELAY_MS` | `app.scheduler.delay-ms` | 30000 | 1000 |
| `APP_INSTALL_POLL_MIN_INTERVAL` | `app.install.poll-min-interval` | PT2M | PT2S |
| `MOCK_ENABLED` | `mock.enabled` | false(운영) | true |
| `MOCK_SUPPLIER_CALLBACK_DELAY_MS` | `mock.supplier.callback-delay-ms` | 3000 | 500 |

- `docker-compose.yml`의 backend·frontend에는 **healthcheck가 있어야** `up --wait`가 동작한다

## 5. 실패했을 때
1. `npx playwright show-report`로 trace(단계별 스냅샷·네트워크)를 연다
2. CI라면 아티팩트 `e2e-report`와 `compose.log`를 받는다
3. 테스트를 고치기 전에 **제품 버그인지 테스트 문제인지** 먼저 판단한다. 기다리는 시간만 늘려서 통과시키지 않는다
