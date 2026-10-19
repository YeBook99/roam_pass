#!/usr/bin/env bash
# 저장소를 clone 한 뒤 한 번 실행: 커밋 템플릿과 git 훅을 이 저장소에 연결한다.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
chmod +x .githooks/* .claude/hooks/*.py 2>/dev/null || true
git config core.hooksPath .githooks
git config commit.template .gitmessage
echo "OK: core.hooksPath=.githooks, commit.template=.gitmessage"
