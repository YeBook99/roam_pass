import { clientGet, clientSend } from '@/lib/api-client';
import type { MockPgSettings, SupplierProfile, SupplierStateRequest } from './types';

// 브라우저에서 부른다. /mock/...도 next.config.ts의 rewrites가 백엔드로 넘긴다

// 보낸 값만 바뀐다(나머지는 그대로)
export function updatePgSettings(settings: Partial<MockPgSettings>): Promise<MockPgSettings> {
  return clientSend<MockPgSettings>('PUT', '/mock/pg/config', { body: settings });
}

// 처음 값으로 되돌린다
export function resetPgSettings(): Promise<MockPgSettings> {
  return clientSend<MockPgSettings>('POST', '/mock/reset');
}

// 주문번호로 SIM 번호를 찾는다(운영 주문 상세 조회). SIM이 아직 없으면 null
export async function findIccid(orderNo: string): Promise<string | null> {
  const detail = await clientGet<{ order: { iccid: string | null } }>(
    `/api/admin/orders/${encodeURIComponent(orderNo)}`,
  );
  return detail.order.iccid;
}

// 가짜 공급사에서 그 SIM을 "설치됨" 또는 "개통됨"으로 바꾼다
export function changeSupplierState(iccid: string, request: SupplierStateRequest): Promise<SupplierProfile> {
  return clientSend<SupplierProfile>('POST', `/mock/supplier/profiles/${encodeURIComponent(iccid)}/state`, {
    body: request,
  });
}
