// CI: 저장소에 추적되는 파일 전체에서 시크릿을 찾는다. 하나라도 있으면 exit 1.
// 사용: node scripts/ci/secret-scan.mjs
import { execSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { basename } from 'node:path';
import { isAllowedPath, isSecretFileName, findSecret } from '../lib/secret-patterns.mjs';

const files = execSync('git ls-files', { encoding: 'utf8' }).split('\n').filter(Boolean);
const findings = [];

for (const f of files) {
  if (isSecretFileName(basename(f))) { findings.push(`${f}: 시크릿 파일이 커밋됨`); continue; }
  if (isAllowedPath(f)) continue;
  let text;
  try { text = readFileSync(f, 'utf8'); } catch { continue; }
  if (text.includes('\u0000')) continue;                 // 바이너리 파일은 건너뜀
  const label = findSecret(text);
  if (label) findings.push(`${f}: ${label}`);
}

if (findings.length) {
  console.error('[secret-scan] 시크릿으로 보이는 내용이 있습니다:');
  for (const x of findings) console.error('  - ' + x);
  process.exit(1);
}
console.log(`[secret-scan] OK (${files.length}개 파일 검사)`);
