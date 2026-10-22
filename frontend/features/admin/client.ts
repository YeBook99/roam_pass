import { clientGet } from '@/lib/api-client';
import type { AdminOrderDetail, AdminOrderSummary, Page } from './types';

// 브라우저에서 부른다 (시연 화면의 "주문 처리 과정 실시간 보기")

// 가장 최근 주문 번호(주문 목록의 첫 줄). 주문이 하나도 없으면 null
export async function findLatestOrderNo(): Promise<string | null> {
  const page = await clientGet<Page<AdminOrderSummary>>('/api/admin/orders?page=0&size=1');
  return page.content[0]?.orderNo ?? null;
}

// 주문 상세 + 타임라인 (server.ts의 getAdminOrder와 같은 주소를 브라우저에서)
export function fetchOrderTrace(orderNo: string): Promise<AdminOrderDetail> {
  return clientGet<AdminOrderDetail>(`/api/admin/orders/${encodeURIComponent(orderNo)}`);
}
