// PreToolUse(Write|Edit|MultiEdit): 시크릿 파일·시크릿 값 쓰기를 차단한다.
// exit 2 + stderr → Claude에게 차단 사유가 전달된다. 규칙은 CI secret-scan과 공유한다.
import { readFileSync } from 'node:fs';
import { basename } from 'node:path';
import { isAllowedPath, isSecretFileName, findSecret } from '../../scripts/lib/secret-patterns.mjs';

const input = JSON.parse(readFileSync(0, 'utf8'));
const ti = input.tool_input ?? {};
const filePath = ti.file_path ?? '';
const name = basename(filePath);

// 이 저장소 밖의 파일(임시 폴더 등)은 커밋될 일이 없으므로 검사하지 않는다
const norm = (s) => s.replace(/\\/g, '/').toLowerCase();
const projectDir = process.env.CLAUDE_PROJECT_DIR;
if (projectDir && !norm(filePath).startsWith(norm(projectDir))) process.exit(0);

const text = [
  ti.content,
  ti.new_string,
  ...(ti.edits ?? []).map((e) => e.new_string),
].filter(Boolean).join('\n');

const block = (reason) => {
  console.error(`[guard-secrets] 차단: ${reason}\n시크릿은 저장소 밖 .env 에 두고, .env.example 에는 자리표시자만 쓰세요.`);
  process.exit(2);
};

if (isSecretFileName(name)) block(`${name} 파일은 직접 만들거나 수정하지 않습니다.`);

if (!isAllowedPath(filePath)) {
  const label = findSecret(text);
  if (label) block(`${label}로 보이는 값이 포함되어 있습니다 (${filePath}).`);
}
process.exit(0);
