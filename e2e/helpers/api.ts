import { APIRequestContext, expect } from '@playwright/test';

// 백엔드·모의 시스템 API 계약 (.claude/skills/e2e/SKILL.md "API 계약"과 일치해야 한다)
export const API = process.env.E2E_API_URL ?? 'http://localhost:8080';
export const ADMIN_ID = 'e2e-admin';
const adminHeaders = { 'X-Admin-Id': ADMIN_ID };

export type SupplierConfig = {
  issueFailRate: number;       // 0~1
  callbackDelayMs: number;
  callbackDuplicates: number;
  callbackDropRate: number;    // 0~1
  shuffleOrder: boolean;
};
export type PgConfig = {
  webhookCount: number;        // 0 = 웹훅 누락
  webhookDelayMs: number;
  cancelFailRate: number;      // 0~1 (5xx)
};

/** 모의 PG·공급사 설정을 기본값으로 되돌린다. 모든 테스트의 beforeEach에서 호출. */
export async function resetMocks(request: APIRequestContext) {
  const res = await request.post(`${API}/mock/reset`);
  expect(res.ok(), 'POST /mock/reset').toBeTruthy();
}

export async function setSupplier(request: APIRequestContext, cfg: Partial<SupplierConfig>) {
  const res = await request.put(`${API}/mock/supplier/config`, { data: cfg });
  expect(res.ok(), 'PUT /mock/supplier/config').toBeTruthy();
}

export async function setPg(request: APIRequestContext, cfg: Partial<PgConfig>) {
  const res = await request.put(`${API}/mock/pg/config`, { data: cfg });
  expect(res.ok(), 'PUT /mock/pg/config').toBeTruthy();
}

/** 시연용: 공급사 프로필 상태를 바꾼다 (실제 기기 설치 흉내). sendCallback=false면 콜백 누락 상황 */
export async function setProfileState(
  request: APIRequestContext, iccid: string,
  state: 'INSTALLED' | 'ENABLED' | 'DELETED', sendCallback: boolean,
) {
  const res = await request.post(`${API}/mock/supplier/profiles/${iccid}/state`, { data: { state, sendCallback } });
  expect(res.ok(), 'POST /mock/supplier/profiles/{iccid}/state').toBeTruthy();
}

export type AdminOrderDetail = {
  orderNo: string;
  status: string;
  payments: { status: string; amount: number }[];
  profile: { iccid: string; installStatus: string } | null;
  timeline: { at: string; target: string; from: string | null; to: string; actor: string; signal: string }[];
};

export async function adminOrder(request: APIRequestContext, orderNo: string): Promise<AdminOrderDetail> {
  const res = await request.get(`${API}/api/admin/orders/${orderNo}`, { headers: adminHeaders });
  expect(res.ok(), 'GET /api/admin/orders/{orderNo}').toBeTruthy();
  return res.json();
}

/** 모의 PG가 이 주문으로 받은 승인 요청 수 (중복 결제 검증용) */
export async function pgApproveCount(request: APIRequestContext, orderNo: string): Promise<number> {
  const res = await request.get(`${API}/mock/pg/payments?orderNo=${orderNo}`);
  expect(res.ok()).toBeTruthy();
  return (await res.json()).length;
}

/** 주문 상태가 기대값이 될 때까지 기다린다 (발급 콜백·스케줄러는 비동기) */
export async function waitOrderStatus(
  request: APIRequestContext, orderNo: string, status: string, timeout = 60_000,
) {
  await expect
    .poll(async () => (await adminOrder(request, orderNo)).status, { timeout, message: `주문 ${orderNo} → ${status}` })
    .toBe(status);
}

export function uniqueUser(prefix: string) {
  return `e2e-${prefix}-${Date.now().toString(36)}`;
}
