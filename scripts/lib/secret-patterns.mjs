// 시크릿 탐지 규칙 — Claude hook(guard-secrets)과 CI(secret-scan)가 같은 규칙을 쓰도록 한 곳에 둔다.

export const SECRET_FILE = /^\.env(\..+)?$/;          // .env, .env.local ... (단 .env.example은 허용)

export const SECRET_PATTERNS = [
  [/-----BEGIN [A-Z ]*PRIVATE KEY-----/, '개인 키'],
  [/AKIA[0-9A-Z]{16}/, 'AWS Access Key'],
  [/aws_secret_access_key\s*=\s*\S{20,}/i, 'AWS Secret Key'],
  [/gh[pousr]_[A-Za-z0-9]{36,}/, 'GitHub 토큰'],
  // password: xxx / secret= xxx 처럼 값이 그대로 박힌 경우 (${ENV} 참조나 자리표시자는 허용)
  [/(password|secret|hmac[-_]?key|api[-_]?key)\s*[:=]\s*["']?(?!\$\{|<|changeme|change-me|your-|example|dummy|test)[A-Za-z0-9+/_\-!@#%^&*]{8,}/i, '하드코딩된 비밀 값'],
];

// 테스트 코드의 고정 테스트 키, 문서 예시, 규칙 파일 자신은 검사하지 않는다
export function isAllowedPath(filePath) {
  const p = filePath.replace(/\\/g, '/');
  return /(^|\/)(src\/test|docs)\//.test(p)
    || p.endsWith('.md')
    || p.endsWith('.env.example')
    || p.endsWith('scripts/lib/secret-patterns.mjs');
}

export function isSecretFileName(name) {
  return SECRET_FILE.test(name) && name !== '.env.example';
}

export function findSecret(text) {
  for (const [re, label] of SECRET_PATTERNS) if (re.test(text)) return label;
  return null;
}
