#!/usr/bin/env bash
# CI: <base>..HEAD 범위의 커밋 메시지가 .gitmessage 템플릿을 지키는지 검사한다.
# 사용: scripts/ci/lint-commits.sh <base-ref>   (예: origin/develop)
# 머지 커밋은 GitHub가 만드는 메시지라 제외한다.
set -euo pipefail

BASE="${1:?base ref 필요 (예: origin/develop)}"
TMP="$(mktemp)"
FAIL=0
COUNT=0

for sha in $(git rev-list --no-merges "${BASE}..HEAD"); do
  COUNT=$((COUNT + 1))
  git log -1 --format=%B "$sha" > "$TMP"
  if ! node .githooks/commit-msg "$TMP" 2>/tmp/lint-err; then
    echo "::error::커밋 $(git log -1 --format='%h %s' "$sha") 형식 오류"
    cat /tmp/lint-err
    FAIL=1
  fi
done

echo "[lint-commits] ${BASE}..HEAD 범위 커밋 ${COUNT}개 검사"
exit "$FAIL"
