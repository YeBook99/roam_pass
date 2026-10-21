// 백엔드는 UTC ISO 문자열을 준다. 화면에 보일 때만 KST로 바꾼다 (CLAUDE.md §3-7)
// 시간대를 고정해 두어서 서버(UTC)와 브라우저가 같은 글자를 그린다 → 하이드레이션 에러가 없다
const KST = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

export function formatKst(iso: string | null | undefined): string {
  return iso ? KST.format(new Date(iso)) : '-';
}
