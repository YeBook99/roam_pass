'use client'; // 2초마다 다시 묻는 타이머가 있어서 브라우저에서 돈다

import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { fetchMyOrder } from '../client';
import { ISSUE_FINAL_STATUSES, ORDER_STATUS_LABEL, type Order } from '../types';

// 동작: 서버가 준 첫 주문 상태(initial)로 화면을 그린 뒤, 2초마다 주문 상태를 다시 묻는다
// 발급 완료·실패 같은 최종 상태가 되면 묻기를 멈춘다. 화면을 떠나면 TanStack Query가 알아서 멈춘다 (docs/08 §4)
export function IssueStatusPoller({ initial }: { initial: Order }) {
  const { data: order, isError } = useQuery({
    queryKey: ['order', initial.orderNo], // 이 데이터의 이름표. 같은 이름표면 같은 데이터로 본다
    queryFn: () => fetchMyOrder(initial.orderNo),
    initialData: initial, // 서버가 준 첫 데이터로 시작 → 첫 화면이 비어 있지 않다
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status && ISSUE_FINAL_STATUSES.includes(status) ? false : 2000; // false = 멈춤
    },
  });

  // 30초가 지나도 안 끝나면 안내 문구를 바꾼다 (docs/08 §4). 화면을 떠나면 타이머를 지운다
  const [slow, setSlow] = useState(false);
  useEffect(() => {
    const timer = setTimeout(() => setSlow(true), 30_000);
    return () => clearTimeout(timer);
  }, []);

  const finished = ISSUE_FINAL_STATUSES.includes(order.status);

  return (
    <section className="stack">
      <p>
        현재 상태: <strong>{ORDER_STATUS_LABEL[order.status]}</strong>
      </p>

      {order.status === 'ISSUED' && <Link href={`/my-sims/${order.orderNo}`}>내 SIM에서 설치 코드 보기</Link>}
      {order.status === 'PICKUP_READY' && <p>유심은 공항에서 받으실 수 있어요.</p>}
      {(order.status === 'ISSUE_FAILED' || order.status === 'REFUNDING') && (
        <p>발급에 실패해 자동 환불을 진행 중입니다.</p>
      )}
      {order.status === 'PENDING' && <p>아직 결제가 확정되지 않았어요. 결제 결과를 확인하고 있습니다.</p>}

      {!finished && (
        <>
          <p>발급은 서버에서 계속 진행됩니다. 이 창을 닫아도 괜찮아요.</p>
          {slow && <p>평소보다 오래 걸리고 있어요. 최대 6분 안에 발급되거나, 실패하면 자동으로 환불됩니다.</p>}
          <Link href={`/my-sims/${order.orderNo}`}>내 SIM에서 나중에 확인하기</Link>
        </>
      )}
      {isError && <p className="error">상태를 불러오지 못했습니다. 잠시 뒤 다시 시도합니다.</p>}
    </section>
  );
}
