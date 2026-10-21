'use client'; // 버튼이 있어서 브라우저에서 돈다

import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation'; // App Router용 (next/router 아님)
import { useState } from 'react';
import { newIdempotencyKey } from '@/lib/idempotency';
import { payOrder } from '../client';

// 동작: [결제하기]를 누르면 멱등키를 붙여 결제를 요청한다
// - 승인(APPROVED)이나 결과 모름(READY)이면 발급 대기 화면으로 간다. 대기 화면이 주문 상태를 계속 확인한다
// - 거절(FAILED)이면 메시지를 보여 주고 새 멱등키를 만든다. 같은 키로 다시 보내면 백엔드가 "거절"을 그대로 돌려주기 때문이다
export function PayButton({ orderNo }: { orderNo: string }) {
  // 화면이 처음 그려질 때 한 번만 만든다 → 연타·네트워크 재시도해도 같은 키라 결제가 한 번만 된다 (docs/06 §6)
  const [idempotencyKey, setIdempotencyKey] = useState(newIdempotencyKey);
  const [declined, setDeclined] = useState(false);
  const router = useRouter();

  const pay = useMutation({
    mutationFn: () => payOrder(orderNo, idempotencyKey),
    onSuccess: (result) => {
      if (result.paymentStatus === 'FAILED') {
        setDeclined(true);
        setIdempotencyKey(newIdempotencyKey()); // 다시 누르면 "새 결제 시도"가 되도록
        return;
      }
      router.push(`/orders/${orderNo}/waiting`);
    },
  });

  return (
    <div className="stack">
      {/* 요청 중에는 버튼을 막는다. 그래도 두 번 가면 같은 멱등키라 백엔드가 한 건으로 처리한다 */}
      <button type="button" disabled={pay.isPending} onClick={() => pay.mutate()}>
        {pay.isPending ? '결제 중...' : '결제하기'}
      </button>
      {declined && !pay.isPending && <p className="error">결제가 거절되었습니다. 다시 시도해 주세요.</p>}
      {pay.isError && <p className="error">{pay.error.message}</p>}
    </div>
  );
}
