// 브랜치 규칙 — git pre-commit hook과 CI(check-branch-name)가 같은 규칙을 쓰도록 한 곳에 둔다.
const PROTECTED = ['main', 'master', 'develop'];
const TYPES = ['feature', 'fix', 'hotfix', 'refactor', 'test', 'docs', 'chore', 'ci', 'release'];
const PATTERN = new RegExp(`^(${TYPES.join('|')})/[a-z0-9][a-z0-9.-]*$`);

module.exports = { PROTECTED, TYPES, PATTERN };
