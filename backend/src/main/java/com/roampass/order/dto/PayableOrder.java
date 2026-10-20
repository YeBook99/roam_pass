package com.roampass.order.dto;

// 결제 기능이 결제를 시작할 때 받아 가는 주문 정보 (API 응답에는 쓰지 않는다)
public record PayableOrder(Long orderId, String orderNo, int amount) {
}
