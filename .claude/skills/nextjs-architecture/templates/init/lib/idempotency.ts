// 멱등키(같은 요청을 한 건으로 묶는 키)를 만든다
// crypto.randomUUID()는 https나 localhost에서만 있다. EC2를 http로 열면 없어서 getRandomValues로 직접 만든다
export function newIdempotencyKey(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
}
