import { formatElapsed } from '@/lib/time';
import { SIGNAL_LABEL, type Signal, type TimelineItem } from '../types';

// "결제를 누가 확정했나"를 타임라인에서 찾는다
// 가장 마지막 결제 요청(결제 생성 → READY) 이후 처음으로 결제가 READY에서 바뀐 줄이 답이다
export type PaymentResolution =
  | { state: 'none' } // 아직 결제 요청이 없음
  | { state: 'pending'; requestedAt: string } // 결제 요청은 했지만 결과를 아직 모름
  | { state: 'decided'; requestedAt: string; decidedAt: string; toStatus: string; signal: Signal };

export function findPaymentResolution(timeline: TimelineItem[]): PaymentResolution {
  const requests = timeline.filter((t) => t.target === 'PAYMENT' && t.fromStatus === null);
  const request = requests[requests.length - 1];
  if (!request) return { state: 'none' };
  const decision = timeline.find(
    (t) => t.target === 'PAYMENT' && t.fromStatus === 'READY' && Date.parse(t.recordedAt) >= Date.parse(request.recordedAt),
  );
  if (!decision) return { state: 'pending', requestedAt: request.recordedAt };
  return {
    state: 'decided',
    requestedAt: request.recordedAt,
    decidedAt: decision.recordedAt,
    toStatus: decision.toStatus,
    signal: decision.signal,
  };
}

// 결제를 확정한 쪽을 한 줄로 보여 준다. 훅이 없어서 서버(운영 주문 상세)·브라우저(시연 화면) 어디서나 쓴다
// nowMs = 결과 모름일 때 경과 시간을 잴 기준 시각 (서버는 조회 시각, 브라우저는 1초마다 바뀌는 지금)
export function PaymentResolutionLine({ timeline, nowMs }: { timeline: TimelineItem[]; nowMs: number }) {
  const resolution = findPaymentResolution(timeline);
  if (resolution.state === 'none') return <p className="muted">아직 결제 요청이 없습니다</p>;
  if (resolution.state === 'pending') {
    return (
      <p className="notice">
        <strong>결제 결과 모름 · 정기 확인 작업 대기 중</strong> · 결제 요청 후{' '}
        {formatElapsed(resolution.requestedAt, nowMs)} (결제 회사 알림이 오거나, 요청 후 1분이 지나면 다음 정기 확인
        작업이 결제 회사에 물어봐 확정)
      </p>
    );
  }
  return (
    <p>
      결제 {resolution.toStatus === 'APPROVED' ? '확정' : '결과'}: <strong>{SIGNAL_LABEL[resolution.signal]}</strong>{' '}
      <span className="muted">
        · {resolution.toStatus} · 결제 요청 후 {formatElapsed(resolution.requestedAt, Date.parse(resolution.decidedAt))}
      </span>
    </p>
  );
}
