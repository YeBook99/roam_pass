#!/usr/bin/env bash
# CI: 저장소에 어떤 부분이 존재하는지 감지해 GITHUB_OUTPUT에 기록한다.
# 아직 backend/frontend가 없는 초기 단계에서도 워크플로가 실패하지 않고 해당 job만 건너뛰게 하기 위함.
set -euo pipefail
out="${GITHUB_OUTPUT:-/dev/stdout}"

has() { [ -e "$1" ] && echo true || echo false; }

{
  echo "backend=$(has backend/build.gradle)"
  echo "frontend=$(has frontend/package.json)"
} | tee -a "$out"
