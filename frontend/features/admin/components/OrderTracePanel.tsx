'use client'; // 2초마다 다시 읽는 타이머와 1초마다 바뀌는 경과 시간이 있어서 브라우저에서 돈다

import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { KstTime } from '@/components/KstTime';
import { ISSUE_FINAL_STATUSES } from '@/features/order/types';
import { fetchOrderTrace, findLatestOrderNo } from '../client';
import { SIGNAL_LABEL, TIMELINE_TARGET_LABEL, type TimelineItem } from '../types';
import { findPaymentResolution, PaymentResolutionLine } from './PaymentResolutionLine';

const POLL_INTERVAL_MS = 2_000;

// 결제가 "결과 모름"에서 승인·실패로 바뀐 줄 = 결제를 확정한 줄
function isPaymentDecision(item: TimelineItem): boolean {
  return item.target === 'PAYMENT' && item.fromStatus === 'READY';
}

// 동작: 주문 하나의 처리 과정(타임라인)을 2초마다 다시 읽어 보여 준다
// - 결제가 아직 결과 모름이면 "정기 확인 작업 대기 중 · 결제 요청 후 N초"를 1초마다 갱신한다
// - 결제를 확정한 줄은 강조하고, 누가(결제 응답·결제 회사 알림·정기 확인 작업) 확정했는지 맨 위에 적는다
// - 주문이 발급 완료·실패 같은 최종 상태가 되면 다시 읽기를 멈춘다
export function OrderTracePanel() {
  const [input, setInput] = useState('');
  const [orderNo, setOrderNo] = useState<string | null>(null);
  const [now, setNow] = useState(() => Date.now());

  const latest = useMutation({
    mutationFn: findLatestOrderNo,
    onSuccess: (found) => {
      if (found) {
        setInput(found);
        setOrderNo(found);
      }
    },
  });

  const { data, isError, error } = useQuery({
    queryKey: ['order-trace', orderNo],
    queryFn: () => fetchOrderTrace(orderNo!),
    enabled: orderNo !== null, // 주문번호를 정하기 전에는 묻지 않는다
    refetchInterval: (query) => {
      const status = query.state.data?.order.orderStatus;
      if (status && ISSUE_FINAL_STATUSES.includes(status)) return false; // 최종 상태면 멈춘다
      return POLL_INTERVAL_MS;
    },
  });

  const waiting = data ? findPaymentResolution(data.timeline).state === 'pending' : false;

  // 결과 모름일 때만 1초마다 지금 시각을 바꿔 경과 시간을 다시 그린다. 화면을 떠나면 타이머를 지운다
  useEffect(() => {
    if (!waiting) return;
    const timer = setInterval(() => setNow(Date.now()), 1_000);
    return () => clearInterval(timer);
  }, [waiting]);

  return (
    <section className="card stack" aria-live="polite">
      <h2>주문 처리 과정 실시간 보기</h2>
      <form
        className="row"
        onSubmit={(e) => {
          e.preventDefault();
          setOrderNo(input.trim());
        }}
      >
        <label htmlFor="trace-order-no" className="field-label">
          주문번호
        </label>
        <input
          id="trace-order-no"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="예: RP4Q3QRTURJ87F"
          required
        />
        <button type="submit">보기</button>
        <button type="button" disabled={latest.isPending} onClick={() => latest.mutate()}>
          가장 최근 주문 보기
        </button>
      </form>
      {latest.data === null && <p className="muted">아직 주문이 없습니다</p>}

      {data && <PaymentResolutionLine timeline={data.timeline} nowMs={now} />}

      {data && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th className="num">언제 (KST)</th>
                <th>무엇이</th>
                <th>누가 · 어떤 신호로</th>
              </tr>
            </thead>
            <tbody>
              {data.timeline.map((item, index) => (
                <tr key={index} className={isPaymentDecision(item) ? 'row-highlight' : undefined}>
                  <td className="num">
                    <KstTime iso={item.recordedAt} seconds />
                  </td>
                  <td>
                    {TIMELINE_TARGET_LABEL[item.target]}{' '}
                    <span className="muted">{item.fromStatus ?? '(생성)'} →</span> {item.toStatus}
                  </td>
                  <td>
                    {item.actor} <span className="muted">· {SIGNAL_LABEL[item.signal]}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {(isError || latest.isError) && <p className="error">{(error ?? latest.error)?.message}</p>}
    </section>
  );
}
