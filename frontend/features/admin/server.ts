import 'server-only';

import { serverGet } from '@/lib/api-server';
import {
  ADMIN_FILTER_KEYS,
  type AdminFilters,
  type AdminOrderDetail,
  type AdminOrderSummary,
  type Page,
} from './types';

// 주문 목록. 값이 있는 필터만 쿼리에 붙인다 (예: ?orderStatus=ISSUED&page=1)
export function getAdminOrders(filters: AdminFilters, page: number): Promise<Page<AdminOrderSummary>> {
  const query = new URLSearchParams();
  for (const key of ADMIN_FILTER_KEYS) {
    const value = filters[key];
    if (value) query.set(key, value);
  }
  query.set('page', String(page));
  query.set('size', '20');
  return serverGet<Page<AdminOrderSummary>>(`/api/admin/orders?${query}`);
}

// 주문 상세 + 타임라인. 없는 주문이면 백엔드가 404를 준다
export function getAdminOrder(orderNo: string): Promise<AdminOrderDetail> {
  return serverGet<AdminOrderDetail>(`/api/admin/orders/${encodeURIComponent(orderNo)}`);
}
