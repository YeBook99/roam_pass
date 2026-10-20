package com.roampass.order.controller;

import com.roampass.order.dto.OrderCreateRequest;
import com.roampass.order.dto.OrderResponse;
import com.roampass.order.service.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// 사용자는 X-User-Id 헤더로 구분한다 (과제 2.2: 회원 시스템은 범위 밖)
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/api/orders")
    public ResponseEntity<OrderResponse> create(@RequestHeader("X-User-Id") String userId,
                                                @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.create(userId, request.planId(), request.travelStartDate());
        return ResponseEntity.created(URI.create("/api/my/orders/" + response.orderNo())).body(response);
    }

    @GetMapping("/api/my/orders/{orderNo}")
    public OrderResponse getMyOrder(@RequestHeader("X-User-Id") String userId, @PathVariable String orderNo) {
        return orderService.getMyOrder(orderNo, userId);
    }
}
