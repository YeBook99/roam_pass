import type { InstallStatus } from '@/features/issue/types'; // 다른 기능은 types만 가져다 쓴다
import type { OrderStatus } from '@/features/order/types';
import type { PaymentStatus } from '@/features/payment/types';
import type { SimType } from '@/features/plan/types';

// 백엔드 global.web.PageResponse와 1:1. fetchedAt = 서버가 DB를 읽은 시각 ("N초 전 데이터"에 쓴다)
export type Page<T> = {
  content: T[];
  page: number; // 0부터 시작
  size: number;
  totalElements: number;
  fetchedAt: string;
};

// 백엔드 admin.dto.AdminOrderSummaryResponse와 1:1. 결제는 그 주문의 최신 결제, SIM이 없으면 installStatus는 null
export type AdminOrderSummary = {
  orderNo: string;
  userId: string;
  simType: SimType;
  amount: number;
  orderStatus: OrderStatus;
  payStatus: PaymentStatus | null;
  installStatus: InstallStatus | null;
  createdAt: string;
  paidAt: string | null;
};

// 백엔드 admin.dto.AdminOrderDetailResponse.Order와 1:1. 결제·SIM 쪽 값은 아직 없으면 null
export type AdminOrder = {
  orderNo: string;
  userId: string;
  simType: SimType;
  amount: number;
  travelStartDate: string;
  orderStatus: OrderStatus;
  createdAt: string;
  paidAt: string | null;
  supplierOrderNo: string | null;
  payStatus: PaymentStatus | null;
  pgTid: string | null;
  approvedAt: string | null;
  iccid: string | null;
  installStatus: InstallStatus | null;
  qrViewedAt: string | null;
  installedAt: string | null;
  activatedAt: string | null;
  expiresAt: string | null;
};

// 백엔드 admin.dto.TimelineItem과 1:1. 한 줄 = 언제(occurredAt 발생, recordedAt 기록), 무엇이(target from→to), 누가/어떤 신호로
export type TimelineItem = {
  target: 'ORDER' | 'PAYMENT' | 'PROFILE';
  fromStatus: string | null; // 처음 만들어질 때는 null
  toStatus: string;
  actor: string; // USER, SYSTEM, PG, SUPPLIER, ADMIN:아이디
  signal: string; // API, WEBHOOK, CALLBACK, POLLING, SCHEDULER, ADMIN, EVENT
  reason: string | null;
  occurredAt: string;
  recordedAt: string;
};

export type AdminOrderDetail = {
  order: AdminOrder;
  timeline: TimelineItem[];
  fetchedAt: string;
};

// 목록 필터. URL 쿼리 이름과 백엔드 요청 파라미터 이름이 같다
export const ADMIN_FILTER_KEYS = ['orderStatus', 'payStatus', 'installStatus', 'simType', 'from', 'to'] as const;
export type AdminFilterKey = (typeof ADMIN_FILTER_KEYS)[number];
export type AdminFilters = Partial<Record<AdminFilterKey, string>>;

export const TIMELINE_TARGET_LABEL: Record<TimelineItem['target'], string> = {
  ORDER: '주문',
  PAYMENT: '결제',
  PROFILE: 'SIM',
};
