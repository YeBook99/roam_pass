'use client';

import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation'; // App Router용 (next/router 아님)
import { useState } from 'react';
import { newIdempotencyKey } from '@/lib/idempotency';
import { payOrder } from '../client';

export function PayButton({ orderNo }: { orderNo: string }) {
  // 화면이 처음 그려질 때 한 번만 만든다 → 연타·재시도해도 같은 키 (docs/06 §6)
  const [idempotencyKey] = useState(newIdempotencyKey);
  const router = useRouter();

  const pay = useMutation({
    mutationFn: () => payOrder(orderNo, idempotencyKey),
    onSuccess: () => router.push(`/orders/${orderNo}/waiting`),
  });

  return (
    <div>
      <button type="button" disabled={pay.isPending} onClick={() => pay.mutate()}>
        {pay.isPending ? '결제 중...' : '결제하기'}
      </button>
      {pay.isError && <p className="error">{pay.error.message}</p>}
    </div>
  );
}
