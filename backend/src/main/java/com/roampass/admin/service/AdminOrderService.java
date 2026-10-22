package com.roampass.admin.service;

import com.roampass.admin.dto.AdminOrderDetailResponse;
import com.roampass.admin.dto.AdminOrderSearch;
import com.roampass.admin.dto.AdminOrderSummaryResponse;
import com.roampass.global.web.PageResponse;

// 어드민 주문 목록·상세 조회. 읽기만 하고 상태는 바꾸지 않는다 (docs/09 §1·§2)
public interface AdminOrderService {

    PageResponse<AdminOrderSummaryResponse> searchOrders(AdminOrderSearch search, int page, int size);

    AdminOrderDetailResponse getOrder(String orderNo);
}
