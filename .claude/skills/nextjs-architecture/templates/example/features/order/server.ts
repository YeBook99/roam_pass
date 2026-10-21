import 'server-only';

import { serverGet } from '@/lib/api-server';
import type { Order } from './types';

// 페이지(Server Component)가 첫 화면을 그릴 때 부른다
export function getMyOrder(orderNo: string): Promise<Order> {
  return serverGet<Order>(`/api/my/orders/${encodeURIComponent(orderNo)}`);
}
