import type { OrderStatus } from '@/features/order/types'; // 다른 기능은 types만 가져다 쓴다

// 백엔드 payment.dto.PaymentResponse와 1:1
// READY = PG에 요청은 보냈지만 결과를 아직 모름(타임아웃 등). 화면은 발급 대기로 가서 주문 상태를 다시 묻는다
export type PaymentStatus = 'READY' | 'APPROVED' | 'FAILED' | 'PARTIAL_CANCELED' | 'CANCELED';

export type PaymentResult = {
  paymentId: number;
  paymentStatus: PaymentStatus;
  orderNo: string;
  orderStatus: OrderStatus;
};
