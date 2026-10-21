import type { OrderStatus } from '@/features/order/types'; // 다른 기능은 types만 가져다 쓴다

// 백엔드 payment.dto.PaymentResponse와 1:1
export type PaymentStatus = 'READY' | 'APPROVED' | 'FAILED' | 'PARTIAL_CANCELED' | 'CANCELED';

export type PaymentResult = {
  paymentId: number;
  paymentStatus: PaymentStatus;
  orderNo: string;
  orderStatus: OrderStatus;
};
