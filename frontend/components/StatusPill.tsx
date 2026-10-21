import { toneOf } from '@/lib/status-tone';

// 상태 하나를 연한 바탕 + 같은 계열의 진한 글자로 보여 준다. 값이 없으면 회색 대시
// label을 주면 그 글자를(사용자 화면의 한글 이름), 없으면 상태 코드를 그대로(어드민) 보여 준다
export function StatusPill({ status, label }: { status: string | null | undefined; label?: string }) {
  if (!status) return <span className="muted">–</span>;
  return <span className={`pill pill-${toneOf(status)}`}>{label ?? status}</span>;
}
