package com.roampass.payment.dto;

import com.roampass.order.domain.OrderStatus;
import com.roampass.payment.domain.Payment;
import com.roampass.payment.domain.PaymentStatus;

// paymentStatus가 READY면 "결제 확인 중"이다. 화면은 주문 상태를 다시 조회한다
public record PaymentResponse(Long paymentId, PaymentStatus paymentStatus, String orderNo, OrderStatus orderStatus) {

    public static PaymentResponse from(Payment payment, String orderNo, OrderStatus orderStatus) {
        return new PaymentResponse(payment.getId(), payment.getStatus(), orderNo, orderStatus);
    }
}
