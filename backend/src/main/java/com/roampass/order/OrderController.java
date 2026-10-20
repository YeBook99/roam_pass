package com.roampass.order;

import com.roampass.plan.SimType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public record CreateOrderRequest(Long planId, LocalDate travelStartDate) {
    }

    public record OrderResponse(String orderNo, OrderStatus status, SimType simType, int amount,
                                LocalDate travelStartDate, Instant expiresAt, Instant paidAt, Instant createdAt) {
        static OrderResponse of(SimOrder o) {
            return new OrderResponse(o.getOrderNo(), o.getStatus(), o.getSimType(), o.getAmount(),
                    o.getTravelStartDate(), o.getExpiresAt(), o.getPaidAt(), o.getCreatedAt());
        }
    }

    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@RequestHeader("X-User-Id") String userId, @RequestBody CreateOrderRequest req) {
        return OrderResponse.of(orderService.create(userId, req.planId(), req.travelStartDate()));
    }

    @GetMapping("/api/my/orders/{orderNo}")
    public OrderResponse get(@RequestHeader("X-User-Id") String userId, @PathVariable String orderNo) {
        return OrderResponse.of(orderService.getMine(orderNo, userId));
    }
}
