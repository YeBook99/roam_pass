// 백엔드는 UTC ISO 문자열을 준다. 화면에 보일 때만 KST로 바꾼다 (CLAUDE.md §3-7)
// 시간대를 고정해 두어서 서버(UTC)와 브라우저가 같은 글자를 그린다 → 하이드레이션 에러가 없다
const KST = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23', // 0~23시. hour12: false로 쓰면 한국어 형식에서 자정이 "24:20"으로 나온다
});

// 어드민 타임라인용. 같은 분 안에 일어난 일의 순서가 보이도록 초까지
const KST_SECONDS = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hourCycle: 'h23',
});

// 'en-CA' 형식은 2026-05-09처럼 나온다. <input type="date">와 백엔드 LocalDate가 이 형식을 쓴다
const KST_DATE = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' });

export function formatKst(iso: string | null | undefined, withSeconds = false): string {
  if (!iso) return '-';
  return (withSeconds ? KST_SECONDS : KST).format(new Date(iso));
}

// 한국 날짜로 오늘. 여행 시작일 기본값·최솟값에 쓴다 (UTC로는 아직 어제일 수 있다)
export function todayKst(): string {
  return KST_DATE.format(new Date());
}

// 두 시각 사이에 걸린 시간을 "1분 12초"처럼 (경과 시간이라 시간대와 관계없다)
export function formatElapsed(fromIso: string, toMs: number): string {
  const sec = Math.max(0, Math.floor((toMs - Date.parse(fromIso)) / 1000));
  return sec >= 60 ? `${Math.floor(sec / 60)}분 ${sec % 60}초` : `${sec}초`;
}
