// PreToolUse(Bash|PowerShell): 커밋 히스토리를 망가뜨리거나 검사를 우회하는 명령을 차단한다.
// 과제 요구: "squash하지 말고 작업 흐름이 보이게 남겨 주세요" (2.5-6)
import { readFileSync } from 'node:fs';

const input = JSON.parse(readFileSync(0, 'utf8'));
const cmd = input.tool_input?.command ?? '';

const rules = [
  [/git\s+push\b.*(--force\b|--force-with-lease\b|\s-f\b)/, 'force push는 커밋 히스토리를 덮어씁니다.'],
  [/--no-verify\b/, '--no-verify는 commit-msg 형식 검사를 우회합니다.'],
  [/git\s+rebase\b.*(-i\b|--interactive\b)/, 'interactive rebase(squash)는 작업 흐름을 지웁니다.'],
  [/git\s+merge\b.*--squash\b/, 'squash merge는 작업 흐름을 지웁니다.'],
  [/git\s+commit\b.*--amend\b/, 'amend 대신 새 커밋으로 수정 흐름을 남기세요.'],
  [/git\s+add\b.*(^|\s)\.env(\s|$)/, '.env는 커밋하지 않습니다.'],
  [/git\s+config\b.*core\.hooksPath\b(?!\s+\.githooks)/, 'commit-msg 훅 경로를 바꾸지 마세요.'],
  // 브랜치 전략: main/develop은 PR로만 변경한다
  [/git\s+push\b[^|;&]*\s(origin\s+)?(HEAD:)?(main|master|develop)(\s|$)/, '보호 브랜치(main/develop)로 직접 push하지 않습니다. 작업 브랜치를 push하고 PR을 올리세요.'],
  [/ALLOW_PROTECTED_COMMIT=/, '보호 브랜치 커밋 우회 변수는 사용자만 쓸 수 있습니다.'],
];

for (const [re, reason] of rules) {
  if (re.test(cmd)) {
    console.error(`[guard-bash] 차단: ${reason}\n명령: ${cmd}\n정말 필요하면 사용자에게 직접 실행을 요청하세요.`);
    process.exit(2);
  }
}
process.exit(0);
