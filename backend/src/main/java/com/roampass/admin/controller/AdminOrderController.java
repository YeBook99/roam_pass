package com.roampass.admin.controller;

import com.roampass.admin.dto.AdminOrderDetailResponse;
import com.roampass.admin.dto.AdminOrderSearch;
import com.roampass.admin.dto.AdminOrderSummaryResponse;
import com.roampass.admin.service.AdminOrderService;
import com.roampass.global.web.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 운영자용 주문 조회. 조회만 하므로 운영자 헤더는 받지 않는다 (조치 API부터 X-Admin-Id를 받는다)
@RestController
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    // 예: /api/admin/orders?orderStatus=ISSUED&simType=ESIM&from=2026-05-01&to=2026-05-09&page=0&size=20
    @GetMapping("/api/admin/orders")
    public PageResponse<AdminOrderSummaryResponse> searchOrders(@ModelAttribute AdminOrderSearch search,
                                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminOrderService.searchOrders(search, page, size);
    }

    @GetMapping("/api/admin/orders/{orderNo}")
    public AdminOrderDetailResponse getOrder(@PathVariable String orderNo) {
        return adminOrderService.getOrder(orderNo);
    }
}
