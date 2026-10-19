#!/usr/bin/env python3
"""PreToolUse(Edit|Write|MultiEdit) 가드.

- .env / .env.* 작성 금지 (.env.example 은 허용)        -> CLAUDE.md I12
- 이미 커밋된 Flyway 마이그레이션 수정 금지              -> CLAUDE.md I11
"""
import json
import os
import re
import subprocess
import sys

try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit(0)

path = data.get("tool_input", {}).get("file_path", "")
if not path:
    sys.exit(0)

name = os.path.basename(path)
if re.fullmatch(r"\.env(\..+)?", name) and name != ".env.example":
    print(
        f"[harness] 차단: {name} 는 작성할 수 없습니다 (시크릿 커밋 방지). "
        "필요한 키 이름은 .env.example 에 값 없이 추가하고, 실제 값은 사용자가 직접 넣도록 안내하세요.",
        file=sys.stderr,
    )
    sys.exit(2)

if re.search(r"/db/migration/V\d+(_\d+)*__[^/]+\.sql$", path):
    root = os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()
    rel = os.path.relpath(os.path.abspath(path), root)
    committed = subprocess.run(
        ["git", "-C", root, "cat-file", "-e", f"HEAD:{rel}"], capture_output=True
    ).returncode == 0
    if committed:
        print(
            f"[harness] 차단: {rel} 는 이미 커밋된 마이그레이션입니다. "
            "수정하지 말고 다음 번호의 새 파일(V{n+1}__설명.sql)을 만드세요. (Flyway checksum 불일치 방지)",
            file=sys.stderr,
        )
        sys.exit(2)

sys.exit(0)
