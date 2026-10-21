import { readJson } from './api-error';
import { USER_COOKIE } from './user';

// 브라우저(Client Component)에서 부른다. /api/...는 next.config.ts의 rewrites가 Spring으로 넘긴다

function userHeader(): Record<string, string> {
  const cookie = document.cookie.split('; ').find((c) => c.startsWith(`${USER_COOKIE}=`));
  return cookie ? { 'X-User-Id': decodeURIComponent(cookie.split('=')[1]) } : {};
}

export async function clientGet<T>(path: string): Promise<T> {
  const res = await fetch(path, { cache: 'no-store', headers: userHeader() });
  return readJson<T>(res);
}

export async function clientSend<T>(
  method: 'POST' | 'PUT',
  path: string,
  options: { body?: unknown; headers?: Record<string, string> } = {},
): Promise<T> {
  const jsonHeader: Record<string, string> =
    options.body === undefined ? {} : { 'Content-Type': 'application/json' };
  const res = await fetch(path, {
    method,
    cache: 'no-store',
    headers: { ...userHeader(), ...jsonHeader, ...options.headers },
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  return readJson<T>(res);
}
