package com.roampass.payment.controller;

import com.roampass.payment.dto.PaymentResponse;
import com.roampass.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/api/orders/{orderNo}/payments")
    public PaymentResponse pay(@RequestHeader("X-User-Id") String userId,
                               @RequestHeader("Idempotency-Key") String idempotencyKey,
                               @PathVariable String orderNo) {
        return paymentService.pay(orderNo, userId, idempotencyKey);
    }
}
