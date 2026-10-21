import { clientSend } from '@/lib/api-client';
import type { PaymentResult } from './types';

// 결제 요청. 같은 멱등키로 다시 보내면 백엔드가 PG를 다시 부르지 않고 처음 결과를 돌려준다 (docs/06 §6)
export function payOrder(orderNo: string, idempotencyKey: string): Promise<PaymentResult> {
  return clientSend<PaymentResult>('POST', `/api/orders/${encodeURIComponent(orderNo)}/payments`, {
    headers: { 'Idempotency-Key': idempotencyKey },
  });
}
