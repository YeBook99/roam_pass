import { clientGet } from '@/lib/api-client';
import type { Order } from './types';

// 브라우저에서 폴링할 때 부른다
export function fetchMyOrder(orderNo: string): Promise<Order> {
  return clientGet<Order>(`/api/my/orders/${encodeURIComponent(orderNo)}`);
}
