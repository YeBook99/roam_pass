import type { Tone } from '@/lib/status-tone';

// 상태 하나를 연한 바탕 + 같은 계열의 진한 글자로 보여 준다. 값이 없으면(label이 null) 회색 대시
// tone은 각 기능 types.ts의 *_STATUS_TONE 표에서 꺼내 넘긴다
export function StatusPill({ tone, label }: { tone: Tone; label: string | null | undefined }) {
  if (!label) return <span className="muted">–</span>;
  return <span className={`pill pill-${tone}`}>{label}</span>;
}
