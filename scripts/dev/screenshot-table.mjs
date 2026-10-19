// .mr-assets/<브랜치>/ 의 <화면>-<뷰포트>-<before|after>.png 를 모아 PR 첨부용 표(SCREENSHOTS.md)를 만든다.
// 사용: node scripts/dev/screenshot-table.mjs <브랜치명>
import { readdirSync, writeFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const branch = process.argv[2];
if (!branch) { console.error('사용: node scripts/dev/screenshot-table.mjs <브랜치명>'); process.exit(1); }

const dir = join('.mr-assets', branch);
if (!existsSync(dir)) { console.error(`${dir} 이(가) 없습니다. 먼저 /ui-screenshot 으로 캡처하세요.`); process.exit(1); }

const shots = {};                                    // key: "화면|뷰포트" → { before, after }
for (const f of readdirSync(dir)) {
  const m = f.match(/^(.+)-(desktop|mobile)-(before|after)\.png$/);
  if (!m) continue;
  const [, page, viewport, phase] = m;
  (shots[`${page}|${viewport}`] ??= {})[phase] = f;
}

const rows = Object.entries(shots).sort().map(([key, s]) => {
  const [page, viewport] = key.split('|');
  const cell = (f) => (f ? `\`${f}\` ← 드래그해서 교체` : '⚠️ 없음');
  return `| ${page} | ${viewport} | ${cell(s.before)} | ${cell(s.after)} |`;
});

const missing = Object.values(shots).filter((s) => !s.before || !s.after).length;
const md = [
  `## 화면 변경 — \`${branch}\``,
  '',
  '| 화면 | 뷰포트 | Before | After |',
  '|---|---|---|---|',
  ...rows,
  '',
  missing ? `> ⚠️ before/after 짝이 맞지 않는 항목 ${missing}개` : '> before/after 모두 캡처됨',
].join('\n');

writeFileSync(join(dir, 'SCREENSHOTS.md'), md + '\n');
console.log(md);
