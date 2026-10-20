package com.roampass.payment;

import com.roampass.order.OrderStatus;
import com.roampass.order.SimOrderRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentFacade paymentFacade;
    private final SimOrderRepository orderRepository;

    public PaymentController(PaymentFacade paymentFacade, SimOrderRepository orderRepository) {
        this.paymentFacade = paymentFacade;
        this.orderRepository = orderRepository;
    }

    // paymentStatus가 READY면 "결제 확인 중"이다. 화면은 주문 상태를 다시 조회한다
    public record PaymentResponse(Long paymentId, PaymentStatus paymentStatus, String orderNo, OrderStatus orderStatus) {
    }

    // Idempotency-Key: 화면이 결제 페이지에 들어올 때 한 번 만든 UUID. 연타·재시도에도 같은 값을 보낸다
    @PostMapping("/api/orders/{orderNo}/payments")
    public PaymentResponse pay(@RequestHeader("X-User-Id") String userId,
                               @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                               @PathVariable String orderNo) {
        Payment p = paymentFacade.pay(orderNo, userId, idempotencyKey);
        OrderStatus orderStatus = orderRepository.findById(p.getOrderId()).orElseThrow().getStatus();
        return new PaymentResponse(p.getId(), p.getStatus(), orderNo, orderStatus);
    }
}