#!/usr/bin/env python3
"""SessionStart: 진행 상황과 작업 규칙을 세션 시작 컨텍스트로 넣는다. (stdout 이 컨텍스트가 됨)"""
import os
import re
import subprocess

root = os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()

def git(*args):
    return subprocess.run(["git", "-C", root, *args], capture_output=True, text=True).stdout.strip()

lines = ["[harness] ROAMPASS 과제 세션 — 규칙: CLAUDE.md / 설계: docs/GUIDE.md"]

progress = os.path.join(root, "docs", "PROGRESS.md")
if os.path.exists(progress):
    items = re.findall(r"^- \[( |x|~)\] (\S+) (.*)$", open(progress, encoding="utf-8").read(), re.M)
    done = sum(1 for s, _, _ in items if s == "x")
    todo = [(i, t) for s, i, t in items if s == " "]
    lines.append(f"진행: {done}/{len(items)} 완료")
    if todo:
        lines.append("다음 할 일 (순서대로):")
        lines += [f"  - {i} {t}" for i, t in todo[:6]]

log = git("log", "--oneline", "-5")
if log:
    lines.append("최근 커밋:")
    lines += [f"  {l}" for l in log.splitlines()]

dirty = git("status", "--porcelain")
if dirty:
    lines.append(f"커밋 안 된 변경 {len(dirty.splitlines())}개 있음 — 이어서 할지 사용자에게 확인")

if git("config", "core.hooksPath") != ".githooks":
    lines.append("주의: git 훅이 연결되지 않음 → 사용자에게 ./scripts/setup-dev.sh 실행을 안내")

lines.append("흐름: REQ 하나씩 → 테스트 → 구현 → PROGRESS/DECISIONS 갱신 → /commit (스킬: /implement-req, /explain, /readme-qa, /submit-check)")
print("\n".join(lines))
