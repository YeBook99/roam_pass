'use client';

import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { fetchMyOrder } from '../client';
import { ISSUE_FINAL_STATUSES, type Order } from '../types';

// 발급 대기: 2초마다 주문 상태를 다시 묻고, 최종 상태가 되면 멈춘다 (docs/08 §4)
export function IssueStatusPoller({ initial }: { initial: Order }) {
  const { data: order, isError } = useQuery({
    queryKey: ['order', initial.orderNo],
    queryFn: () => fetchMyOrder(initial.orderNo),
    initialData: initial, // 서버가 준 첫 데이터로 시작 → 깜빡임 없음
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status && ISSUE_FINAL_STATUSES.includes(status) ? false : 2000;
    },
  });

  return (
    <section>
      <p>
        현재 상태: <strong>{order.status}</strong>
      </p>
      {order.status === 'ISSUED' && <Link href={`/my-sims/${order.orderNo}`}>내 SIM 보기</Link>}
      {!ISSUE_FINAL_STATUSES.includes(order.status) && (
        <p>발급은 서버에서 계속 진행됩니다. 이 창을 닫아도 괜찮아요.</p>
      )}
      {isError && <p className="error">상태를 불러오지 못했습니다. 잠시 뒤 다시 시도합니다.</p>}
    </section>
  );
}
