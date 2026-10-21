import { formatKst } from '@/lib/time';

// 시각 하나를 KST로 보여 준다. 훅이 없어서 서버·브라우저 어디서나 쓸 수 있다
export function KstTime({ iso }: { iso: string | null | undefined }) {
  if (!iso) return <span>-</span>;
  return <time dateTime={iso}>{formatKst(iso)}</time>;
}
