// 로그인 시스템이 없어서(과제 2.2) 첫 화면에서 입력한 아이디를 쿠키에 담아 X-User-Id 헤더로 보낸다
// 서버는 cookies()로(lib/api-server.ts), 브라우저는 document.cookie로(lib/api-client.ts) 읽는다
export const USER_COOKIE = 'roampass_user';

// 브라우저에서만 부른다(document가 브라우저에만 있다). 30일 동안 유지
export function saveUserId(userId: string): void {
  document.cookie = `${USER_COOKIE}=${encodeURIComponent(userId)}; path=/; max-age=${60 * 60 * 24 * 30}`;
}
