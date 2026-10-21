import { clientGet, clientSend } from '@/lib/api-client';
import type { Order, OrderCreateRequest } from './types';

// 브라우저에서 부르는 함수들. 주소는 /api/...이고 next.config.ts의 rewrites가 Spring으로 넘긴다

// 발급 대기 화면이 2초마다 부른다
export function fetchMyOrder(orderNo: string): Promise<Order> {
  return clientGet<Order>(`/api/my/orders/${encodeURIComponent(orderNo)}`);
}

// 주문 만들기. 성공하면 결제 대기(PENDING) 주문이 온다
export function createOrder(request: OrderCreateRequest): Promise<Order> {
  return clientSend<Order>('POST', '/api/orders', { body: request });
}
