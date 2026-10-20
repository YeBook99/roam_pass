package com.roampass.order.dto;

import com.roampass.order.domain.OrderStatus;
import com.roampass.order.domain.SimOrder;
import com.roampass.plan.domain.SimType;
import java.time.Instant;
import java.time.LocalDate;

public record OrderResponse(String orderNo, OrderStatus status, SimType simType, int amount,
                            LocalDate travelStartDate, Instant expiresAt, Instant paidAt, Instant createdAt) {

    public static OrderResponse from(SimOrder order) {
        return new OrderResponse(order.getOrderNo(), order.getStatus(), order.getSimType(), order.getAmount(),
                order.getTravelStartDate(), order.getExpiresAt(), order.getPaidAt(), order.getCreatedAt());
    }
}
