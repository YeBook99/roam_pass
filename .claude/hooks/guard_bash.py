#!/usr/bin/env python3
"""PreToolUse(Bash) 가드: 작업 흐름·시크릿 규칙을 깨는 git 명령을 막는다.

exit 2 + stderr 메시지 = 차단 (Claude 에게 사유가 전달됨). 그 외는 통과.
"""
import json
import re
import sys

try:
    cmd = json.load(sys.stdin).get("tool_input", {}).get("command", "")
except Exception:
    sys.exit(0)

# heredoc 본문(커밋 메시지 등)은 명령이 아니라 데이터이므로 검사에서 뺀다.
cmd = re.sub(r"<<-?\s*['\"]?(\w+)['\"]?[^\n]*\n.*?\n\1(?=\n|$)", "", cmd, flags=re.S)

# [^;&|\n]* : 같은 줄, 같은 명령 조각 안에서만 매칭 (다른 명령의 인자를 섞어 읽지 않도록)
SEG = r"[^;&|\n]*"
RULES = [
    (rf"\bgit\b{SEG}--no-verify",
     "--no-verify 금지: git 훅(커밋 템플릿·시크릿 검사)을 우회하지 마세요. 훅이 막은 이유를 고치세요."),
    (rf"\bgit\b{SEG}\bpush\b{SEG}(\s-f\b|--force)",
     "force push 금지: 커밋 히스토리가 평가 대상입니다. 필요하면 사용자에게 먼저 확인하세요."),
    (rf"\bgit\b{SEG}\bmerge\b{SEG}--squash",
     "squash 금지: 과제 2.5-6 '작업 흐름이 보이게 커밋을 남겨 주세요'."),
    (rf"\bgit\b{SEG}\brebase\b(?!{SEG}--(abort|continue|skip))",
     "rebase 금지: 히스토리를 다시 쓰면 작업 흐름이 사라집니다. 필요하면 사용자에게 먼저 확인하세요."),
    (rf"\bgit\b{SEG}\badd\b{SEG}(^|[\s/])\.env(?!\.example)(\.[\w.-]+)?(\s|$)",
     ".env 파일은 커밋할 수 없습니다 (과제 감점: 시크릿 커밋). .env.example 만 커밋하세요."),
    (rf"\bgit\b{SEG}\bconfig\b{SEG}(core\.hooksPath|commit\.template)",
     "git 훅/커밋 템플릿 설정 변경 금지: scripts/setup-dev.sh 가 관리합니다."),
]

for pattern, message in RULES:
    if re.search(pattern, cmd):
        # setup 스크립트 자체 실행은 허용
        if "setup-dev.sh" in cmd:
            continue
        print(f"[harness] 차단: {message}\n  명령: {cmd[:200]}", file=sys.stderr)
        sys.exit(2)

sys.exit(0)
