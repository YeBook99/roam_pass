import type { OrderStatus } from '@/features/order/types'; // 다른 기능은 types만 가져다 쓴다
import type { Tone } from '@/lib/status-tone';

// 백엔드 payment.dto.PaymentResponse와 1:1
// READY = PG에 요청은 보냈지만 결과를 아직 모름(타임아웃 등). 화면은 발급 대기로 가서 주문 상태를 다시 묻는다
export type PaymentStatus = 'READY' | 'APPROVED' | 'FAILED' | 'PARTIAL_CANCELED' | 'CANCELED';

// 어드민 필터 선택지에 쓴다. Record라서 상태가 하나라도 빠지면 빌드가 실패한다
export const PAYMENT_STATUS_LABEL: Record<PaymentStatus, string> = {
  READY: '결과 확인 중',
  APPROVED: '승인',
  FAILED: '거절',
  PARTIAL_CANCELED: '부분 취소',
  CANCELED: '취소',
};

export const PAYMENT_STATUS_TONE: Record<PaymentStatus, Tone> = {
  READY: 'warning',
  APPROVED: 'positive',
  FAILED: 'negative',
  PARTIAL_CANCELED: 'neutral',
  CANCELED: 'neutral',
};

export type PaymentResult = {
  paymentId: number;
  paymentStatus: PaymentStatus;
  orderNo: string;
  orderStatus: OrderStatus;
};
