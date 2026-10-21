import 'server-only'; // 브라우저 코드가 이 파일을 import하면 빌드가 실패한다

import { cookies } from 'next/headers';
import { notFound } from 'next/navigation';
import { ApiError, readJson } from './api-error';
import { USER_COOKIE } from './user';

// 서버(Server Component)에서 Spring을 직접 부른다. 브라우저로 가지 않는 주소라 NEXT_PUBLIC_을 붙이지 않는다
const BASE_URL = process.env.API_INTERNAL_URL ?? 'http://localhost:8080';

// 서버는 조회만 한다. 쓰기(결제, 버튼)는 브라우저에서 api-client로 보낸다
export async function serverGet<T>(path: string): Promise<T> {
  const userId = (await cookies()).get(USER_COOKIE)?.value;
  const res = await fetch(BASE_URL + path, {
    cache: 'no-store', // 결제·발급 상태는 항상 최신이어야 한다
    headers: userId ? { 'X-User-Id': userId } : {},
  });
  return readJson<T>(res);
}

// 404면 Next의 not-found 화면을 보여 준다. 남의 주문도 백엔드가 404로 답한다
export async function orNotFound<T>(promise: Promise<T>): Promise<T> {
  try {
    return await promise;
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }
}
