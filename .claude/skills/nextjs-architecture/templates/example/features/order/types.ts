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
  simType: 'ESIM' | 'USIM';
  amount: number;
  travelStartDate: string; // KST 날짜 (2026-05-09)
  expiresAt: string; // 결제 대기 만료, UTC ISO
  paidAt: string | null;
  createdAt: string;
};

// 발급 대기 화면이 폴링을 멈추는 상태 (docs/08 §4)
export const ISSUE_FINAL_STATUSES: OrderStatus[] = [
  'ISSUED',
  'PICKUP_READY',
  'ISSUE_FAILED',
  'REFUNDING',
  'REFUNDED',
  'REFUND_FAILED',
  'EXPIRED',
];
