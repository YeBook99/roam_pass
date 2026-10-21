import type { SimType } from '@/features/plan/types'; // 다른 기능은 types만 가져다 쓴다

// 백엔드 order.dto.OrderResponse와 1:1. 백엔드가 바뀌면 같이 고친다
export type OrderStatus =
  | 'PENDING'
  | 'EXPIRED'
  | 'PAID'
  | 'ISSUING'
  | 'ISSUED'
  | 'PICKUP_READY'
  | 'ACTIVATING'
  | 'ISSUE_FAILED'
  | 'REFUNDING'
  | 'REFUNDED'
  | 'REFUND_FAILED';

export type Order = {
  orderNo: string;
  status: OrderStatus;
  simType: SimType;
  amount: number;
  travelStartDate: string; // KST 날짜 (2026-05-09)
  expiresAt: string; // 결제 대기 만료, UTC ISO
  paidAt: string | null;
  createdAt: string;
};

// 주문 만들기 요청 (백엔드 OrderCreateRequest)
export type OrderCreateRequest = {
  planId: number;
  travelStartDate: string;
};

// 사용자에게 보여 줄 주문 상태 이름
export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: '결제 대기',
  EXPIRED: '결제 기한 지남',
  PAID: '결제 완료',
  ISSUING: '발급 중',
  ISSUED: '발급 완료',
  PICKUP_READY: '공항 수령 대기',
  ACTIVATING: '개통 중',
  ISSUE_FAILED: '발급 실패',
  REFUNDING: '환불 중',
  REFUNDED: '환불 완료',
  REFUND_FAILED: '환불 지연',
};

// 발급 대기 화면이 폴링을 멈추는 상태. 이 상태가 되면 더 기다려도 바뀌지 않는다 (docs/08 §4)
export const ISSUE_FINAL_STATUSES: OrderStatus[] = [
  'ISSUED',
  'PICKUP_READY',
  'ISSUE_FAILED',
  'REFUNDING',
  'REFUNDED',
  'REFUND_FAILED',
  'EXPIRED',
];
