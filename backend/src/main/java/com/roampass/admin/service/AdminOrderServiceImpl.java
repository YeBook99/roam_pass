package com.roampass.admin.service;

import com.roampass.admin.dto.AdminOrderDetailResponse;
import com.roampass.admin.dto.AdminOrderFilter;
import com.roampass.admin.dto.AdminOrderSearch;
import com.roampass.admin.dto.AdminOrderSummaryResponse;
import com.roampass.admin.dto.TimelineItem;
import com.roampass.admin.exception.AdminErrorCode;
import com.roampass.admin.repository.AdminOrderQueryRepository;
import com.roampass.global.config.ClockConfig;
import com.roampass.global.error.BusinessException;
import com.roampass.global.web.PageResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// AdminOrderService 구현. 설명은 인터페이스 AdminOrderService에 있다
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final AdminOrderQueryRepository queryRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummaryResponse> searchOrders(AdminOrderSearch search, int page, int size) {
        AdminOrderFilter filter = toFilter(search);
        List<AdminOrderSummaryResponse> content = queryRepository.search(filter, page, size);
        long total = queryRepository.count(filter);
        return new PageResponse<>(content, page, size, total, Instant.now(clock));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrder(String orderNo) {
        AdminOrderDetailResponse.Order order = queryRepository.findOrder(orderNo)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.ORDER_NOT_FOUND));
        List<TimelineItem> timeline = queryRepository.findTimeline(orderNo);
        return new AdminOrderDetailResponse(order, timeline, Instant.now(clock));
    }

    // 화면의 기간은 KST 날짜다. KST 5/9 하루 = UTC 5/8 15:00 ~ 5/9 15:00 (끝은 포함하지 않음)
    private AdminOrderFilter toFilter(AdminOrderSearch search) {
        if (search.from() != null && search.to() != null && search.from().isAfter(search.to())) {
            throw new BusinessException(AdminErrorCode.INVALID_DATE_RANGE);
        }
        Instant createdFrom = search.from() == null ? null : startOfKstDay(search.from());
        Instant createdBefore = search.to() == null ? null : startOfKstDay(search.to().plusDays(1));
        return new AdminOrderFilter(
                search.orderStatus() == null ? null : search.orderStatus().name(),
                search.payStatus() == null ? null : search.payStatus().name(),
                search.installStatus() == null ? null : search.installStatus().name(),
                search.simType() == null ? null : search.simType().name(),
                createdFrom, createdBefore);
    }

    private Instant startOfKstDay(LocalDate date) {
        return date.atStartOfDay(ClockConfig.KST).toInstant();
    }
}
