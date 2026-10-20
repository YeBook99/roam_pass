#!/usr/bin/env bash
# CI: <base>..HEAD 범위의 커밋 메시지가 .gitmessage 템플릿을 지키는지 검사한다.
# 사용: scripts/ci/lint-commits.sh <base-ref>   (예: origin/develop)
# 머지 커밋은 GitHub가 만드는 메시지라 제외한다.
# LEGACY_BASELINE과 그 이전 커밋도 제외한다. 템플릿 규칙이 생기기 전에 만든 커밋이라
# 지금 규칙으로는 통과할 수 없고, 이미 develop에 합쳐져 force push 없이는 고칠 수 없기 때문이다.
set -euo pipefail

BASE="${1:?base ref 필요 (예: origin/develop)}"
LEGACY_BASELINE="d68642a846619642f65b43307c6e8a8012fd7d68"   # chore(harness): 개발 워크플로 스킬 추가
TMP="$(mktemp)"
FAIL=0
COUNT=0

EXCLUDE=()
if git cat-file -e "${LEGACY_BASELINE}^{commit}" 2>/dev/null; then
  EXCLUDE=("^${LEGACY_BASELINE}")
fi

for sha in $(git rev-list --no-merges "${BASE}..HEAD" "${EXCLUDE[@]}"); do
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
