package com.roampass.issue.service;

import com.roampass.history.domain.Signal;
import com.roampass.order.domain.OrderPaidEvent;
import com.roampass.order.service.OrderService;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 발급 안전망. 결제 확정 이벤트가 사라졌을 때(그 순간 서버 재시작 등) PAID로 남은 주문을 다시 처리한다 (docs/04 §5)
// "할 일 목록"이 메모리가 아니라 DB의 주문 상태라서 서버가 재시작돼도 이어서 처리된다
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueScheduler {

    private final OrderService orderService;
    private final IssueService issueService;
    private final IssueProperties issueProperties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.issue.scheduler-delay-ms}")
    public void pickUpPaidOrders() {
        Instant before = Instant.now(clock).minus(issueProperties.paidPickupAfter());
        for (OrderPaidEvent paid : orderService.getPaidOrdersBefore(before)) {
            try {
                issueService.handlePaidOrder(paid.orderId(), paid.simType(), Signal.SCHEDULER);
            } catch (RuntimeException e) {
                log.error("발급 안전망 처리 실패, 다음 주기에 다시 시도 orderId={}", paid.orderId(), e);
            }
        }
    }
}
