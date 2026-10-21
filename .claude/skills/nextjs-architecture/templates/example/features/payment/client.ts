import { clientSend } from '@/lib/api-client';
import type { PaymentResult } from './types';

// 같은 멱등키로 다시 보내면 백엔드가 PG를 다시 부르지 않고 지금 상태를 돌려준다
export function payOrder(orderNo: string, idempotencyKey: string): Promise<PaymentResult> {
  return clientSend<PaymentResult>('POST', `/api/orders/${encodeURIComponent(orderNo)}/payments`, {
    headers: { 'Idempotency-Key': idempotencyKey },
  });
}
