#!/usr/bin/env python3
"""PostToolUse(Edit|Write|MultiEdit): 정합성이 걸린 코드를 고치면 관련 불변 규칙을 상기시킨다.

같은 세션에서 같은 파일은 한 번만 알린다 (컨텍스트 낭비 방지).
"""
import json
import os
import re
import sys

try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit(0)

path = data.get("tool_input", {}).get("file_path", "")
session = re.sub(r"[^\w-]", "", data.get("session_id", "default")) or "default"
if not path:
    sys.exit(0)

CHECKS = [
    (r"backend/src/main/.*/(payment|mock/pg|callback)/",
     "결제 코드: I1 PG 호출은 트랜잭션 밖 / I2 상태 전이는 조건부 UPDATE(영향 행 수) / "
     "I4 승인 응답·웹훅·대사는 같은 반영 메서드 / I5 웹훅 서명 + event_key 중복 / I6 금액 불일치 승인 금지 / "
     "관련 테스트: PaymentWebhookConcurrencyTest, PaymentAmountMismatchTest"),
    (r"backend/src/main/.*/(issuance|mock/supplier|usim)/",
     "발급 코드: I1 공급사 호출은 트랜잭션 밖 / I5 원본 body HMAC + MessageDigest.isEqual / "
     "I7 PAID 에는 항상 job / 우리 order_no 를 공급사 멱등 키로 / 프로필은 order_id·iccid·supplier_order_no UNIQUE / "
     "관련 테스트: SupplierCallbackConcurrencyTest, IssuanceFailureCompensationTest"),
    (r"backend/src/main/.*/(refund|job)/",
     "재시도 코드: I8 attempt_count/max_attempts/백오프, 최대 초과 시 DEAD·FAILED_FINAL + 이상 건 / "
     "SKIP LOCKED 로 선점 후 커밋하고 외부 호출 / 재시도 전 PG 조회로 이중 취소 방지 / 관련 테스트: RefundRetryLimitTest"),
    (r"backend/src/main/.*/(tracking)/",
     "설치 추적 코드: I9 install_rank 단방향(WHERE install_rank < :new) / I10 Instant·UTC 저장, 만료 = activated_at + days×24h / "
     "콜백·폴링은 같은 반영 메서드 / 관련 테스트: InstallStatusMonotonicTest, ExpiryCalculatorTest"),
    (r"backend/src/main/.*/(admin)/",
     "어드민 조치: 조건부 UPDATE 로 동시 클릭 1회 / status_history 에 ADMIN:{id} + 사유 / 관련 테스트: AdminConcurrentRefundTest"),
    (r"backend/src/main/resources/db/migration/",
     "마이그레이션: I11 커밋 후 수정 금지 / 시각 컬럼 DATETIME(6) UTC / 중복 방지는 UNIQUE 제약으로 / utf8mb4"),
    (r"backend/src/main/resources/application.*\.(ya?ml|properties)$",
     "설정: I12 시크릿은 ${ENV_VAR} 만 / hibernate.jdbc.time_zone=UTC 유지"),
]

hit = next((msg for pattern, msg in CHECKS if re.search(pattern, path.replace(os.sep, "/"))), None)
if not hit:
    sys.exit(0)

root = os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()
cache_dir = os.path.join(root, ".claude", ".cache")
os.makedirs(cache_dir, exist_ok=True)
cache_file = os.path.join(cache_dir, f"seen-{session}.txt")
seen = set(open(cache_file).read().splitlines()) if os.path.exists(cache_file) else set()
if path in seen:
    sys.exit(0)
with open(cache_file, "a") as f:
    f.write(path + "\n")

print(json.dumps({
    "hookSpecificOutput": {
        "hookEventName": "PostToolUse",
        "additionalContext": f"[harness] {os.path.basename(path)} 수정 — CLAUDE.md 불변 규칙 확인: {hit}. "
                             "결정이 생겼다면 docs/DECISIONS.md 에, 테스트는 같은/다음 커밋에.",
    }
}, ensure_ascii=False))
