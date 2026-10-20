// CI: 수동 실행 대상 브랜치 이름이 규칙을 따르는지 검사한다.
// 사용: node scripts/ci/check-branch-name.mjs <branch>
import { createRequire } from 'node:module';
const { PROTECTED, PATTERN, TYPES } = createRequire(import.meta.url)('../lib/branch-rules.cjs');

const branch = process.argv[2] ?? '';
if (PROTECTED.includes(branch)) {
  console.log(`[branch] ${branch}: 보호 브랜치 (검사 생략)`);
} else if (!PATTERN.test(branch)) {
  console.error(`[branch] '${branch}'이(가) <type>/<scope>-<설명> 규칙과 다릅니다. type: ${TYPES.join(', ')}`);
  process.exit(1);
} else {
  console.log(`[branch] ${branch}: OK`);
}
