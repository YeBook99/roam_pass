'use client'; // 2초마다 다시 묻는 타이머가 있어서 브라우저에서 돈다

import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { fetchMyOrder } from '../client';
import { ISSUE_FINAL_STATUSES, ORDER_STATUS_LABEL, type Order } from '../types';

const POLL_INTERVAL_MS = 2_000;
const SLOW_AFTER_MS = 30_000; // 이 시간이 지나도 안 끝나면 안내 문구를 바꾼다
const GIVE_UP_AFTER_MS = 5 * 60_000; // 이 시간이 지나면 자동 확인을 멈춘다 (창을 열어 둔 채 끝없이 묻지 않게)

// 동작: 서버가 준 첫 주문 상태(initial)로 화면을 그린 뒤, 2초마다 주문 상태를 다시 묻는다
// - 발급 완료·실패 같은 최종 상태가 되면 묻기를 멈춘다
// - 5분이 지나도 안 끝나면 멈추고 "내 SIM에서 확인"을 안내한다 (발급은 서버가 계속 진행한다)
// - 화면을 떠나면 TanStack Query가 알아서 멈춘다 (docs/08 §4)
export function IssueStatusPoller({ initial }: { initial: Order }) {
  const [startedAt] = useState(() => Date.now()); // 이 화면을 연 시각 (처음 한 번만)
  const [slow, setSlow] = useState(false);
  const [gaveUp, setGaveUp] = useState(false);

  // 시간이 지나면 상태를 바꾸는 타이머 두 개. 화면을 떠나면(return 함수) 타이머를 지운다
  useEffect(() => {
    const slowTimer = setTimeout(() => setSlow(true), SLOW_AFTER_MS);
    const giveUpTimer = setTimeout(() => setGaveUp(true), GIVE_UP_AFTER_MS);
    return () => {
      clearTimeout(slowTimer);
      clearTimeout(giveUpTimer);
    };
  }, []);

  const { data: order, isError } = useQuery({
    queryKey: ['order', initial.orderNo], // 이 데이터의 이름표. 같은 이름표면 같은 데이터로 본다
    queryFn: () => fetchMyOrder(initial.orderNo),
    initialData: initial, // 서버가 준 첫 데이터로 시작 → 첫 화면이 비어 있지 않다
    // 다음에 언제 다시 물을지 정한다. false를 돌려주면 멈춘다
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status && ISSUE_FINAL_STATUSES.includes(status)) return false; // 최종 상태
      if (Date.now() - startedAt > GIVE_UP_AFTER_MS) return false; // 너무 오래 걸림
      return POLL_INTERVAL_MS;
    },
  });

  const finished = ISSUE_FINAL_STATUSES.includes(order.status);

  return (
    <section className="stack">
      <p>
        현재 상태: <strong>{ORDER_STATUS_LABEL[order.status]}</strong>
      </p>

      {order.status === 'ISSUED' && <Link href={`/my-sims/${order.orderNo}`}>내 SIM에서 설치 코드 보기</Link>}
      {order.status === 'PICKUP_READY' && <p>유심은 공항에서 받으실 수 있어요.</p>}
      {order.status === 'ISSUE_FAILED' && <p>발급에 실패했습니다.</p>}
      {order.status === 'REFUNDING' && <p>환불을 진행하고 있습니다.</p>}
      {order.status === 'PENDING' && <p>아직 결제가 확정되지 않았어요. 결제 결과를 확인하고 있습니다.</p>}

      {!finished && (
        <>
          <p>발급은 서버에서 계속 진행됩니다. 이 창을 닫아도 괜찮아요.</p>
          {slow && !gaveUp && <p>평소보다 오래 걸리고 있어요. 조금만 더 기다려 주세요.</p>}
          {gaveUp && <p>자동 확인을 멈췄어요. 잠시 뒤 내 SIM에서 다시 확인해 주세요.</p>}
          <Link href={`/my-sims/${order.orderNo}`}>내 SIM에서 나중에 확인하기</Link>
        </>
      )}
      {isError && <p className="error">상태를 불러오지 못했습니다. 잠시 뒤 다시 시도합니다.</p>}
    </section>
  );
}
