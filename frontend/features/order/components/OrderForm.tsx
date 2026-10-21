'use client'; // 선택·입력·버튼이 있어서 브라우저에서 돈다

import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation'; // App Router용 (next/router 아님)
import { useState } from 'react';
import { SIM_TYPE_LABEL, type Plan } from '@/features/plan/types';
import { createOrder } from '../client';

type Props = {
  plans: Plan[]; // 서버가 조회해서 넘겨준 요금제 목록
  todayKst: string; // 서버가 계산한 한국 날짜 오늘 (브라우저 시계에 의존하지 않게)
  disabled: boolean; // 아이디를 아직 입력하지 않았으면 true
};

// 동작: 요금제와 여행 시작일을 고르고 [주문하기]를 누르면
// POST /api/orders → 성공하면 결제 화면(/checkout/주문번호)으로 이동한다
export function OrderForm({ plans, todayKst, disabled }: Props) {
  const [planId, setPlanId] = useState(plans[0]?.id ?? 0);
  const [travelStartDate, setTravelStartDate] = useState(todayKst);
  const router = useRouter();

  // useMutation: "버튼을 눌러서 서버 데이터를 바꾸는 요청"을 다룬다. 요청 중인지(isPending), 에러가 났는지를 알려 준다
  const order = useMutation({
    mutationFn: () => createOrder({ planId, travelStartDate }),
    onSuccess: (created) => router.push(`/checkout/${created.orderNo}`),
  });

  return (
    <form
      className="stack"
      onSubmit={(e) => {
        e.preventDefault();
        order.mutate();
      }}
    >
      <label>
        요금제{' '}
        <select value={planId} onChange={(e) => setPlanId(Number(e.target.value))}>
          {plans.map((plan) => (
            <option key={plan.id} value={plan.id}>
              {plan.country} {plan.days}일 {SIM_TYPE_LABEL[plan.simType]} · {plan.price.toLocaleString('ko-KR')}원
            </option>
          ))}
        </select>
      </label>
      <label>
        여행 시작일(한국 날짜){' '}
        <input
          type="date"
          value={travelStartDate}
          min={todayKst}
          onChange={(e) => setTravelStartDate(e.target.value)}
        />
      </label>
      {/* 요청 중에는 버튼을 막아서 두 번 눌러 주문이 두 개 생기지 않게 한다 */}
      <button type="submit" className="btn-primary" disabled={disabled || order.isPending}>
        {order.isPending ? '주문 중...' : '주문하기'}
      </button>
      {disabled && <p>먼저 위에서 아이디를 저장해 주세요.</p>}
      {order.isError && <p className="error">{order.error.message}</p>}
    </form>
  );
}
