// 브랜치 규칙 — git pre-commit hook이 쓴다.
const PROTECTED = ['main', 'master', 'develop'];
const TYPES = ['feature', 'fix', 'hotfix', 'refactor', 'test', 'docs', 'chore', 'ci', 'release'];
const PATTERN = new RegExp(`^(${TYPES.join('|')})/[a-z0-9][a-z0-9.-]*$`);

module.exports = { PROTECTED, TYPES, PATTERN };
