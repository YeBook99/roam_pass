import { formatKst } from '@/lib/time';

// 시각 하나를 KST로 보여 준다. 훅이 없어서 서버·브라우저 어디서나 쓸 수 있다
// seconds를 주면 초까지 보여 준다 (어드민 타임라인)
export function KstTime({ iso, seconds = false }: { iso: string | null | undefined; seconds?: boolean }) {
  if (!iso) return <span>-</span>;
  return <time dateTime={iso}>{formatKst(iso, seconds)}</time>;
}
