// 상태 문자열 → 표시 색의 종류. 색은 의미가 있을 때만 쓴다 (디자인 기준: 틀은 무채색, 색은 상태와 데이터에만)
// positive = 끝났고 정상, negative = 사람이 봐야 하는 실패, warning = 아직 진행 중, neutral = 그 밖
export type Tone = 'positive' | 'negative' | 'warning' | 'neutral';

const TONE: Record<string, Tone> = {
  // 주문
  PAID: 'warning',
  ISSUING: 'warning',
  ACTIVATING: 'warning',
  REFUNDING: 'warning',
  PENDING: 'warning',
  ISSUED: 'positive',
  PICKUP_READY: 'positive',
  ISSUE_FAILED: 'negative',
  REFUND_FAILED: 'negative',
  // 결제
  READY: 'warning',
  APPROVED: 'positive',
  FAILED: 'negative',
  // 설치
  INSTALLED: 'positive',
  ACTIVE: 'positive',
};

export function toneOf(status: string | null | undefined): Tone {
  return (status && TONE[status]) || 'neutral';
}
