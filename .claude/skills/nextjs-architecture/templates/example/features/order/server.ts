import 'server-only';

import { serverGet } from '@/lib/api-server';
import type { Order } from './types';

// 페이지(Server Component)가 첫 화면을 그릴 때 부른다. 남의 주문이면 백엔드가 404를 준다
export function getMyOrder(orderNo: string): Promise<Order> {
  return serverGet<Order>(`/api/my/orders/${encodeURIComponent(orderNo)}`);
}

// 내 SIM 목록 화면. 최신 주문이 먼저 온다
export function getMyOrders(): Promise<Order[]> {
  return serverGet<Order[]>('/api/my/orders');
}
