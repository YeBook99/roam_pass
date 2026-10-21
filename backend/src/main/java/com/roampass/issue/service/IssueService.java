package com.roampass.issue.service;

import com.roampass.infra.supplier.SupplierClient;
import com.roampass.order.domain.OrderPaidEvent;
import com.roampass.order.service.OrderService;
import com.roampass.plan.domain.SimType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 발급 요청. 결제가 끝나면 서버가 알아서 공급사에 발급을 요청한다 (클라이언트가 부르지 않는다, CLAUDE.md §3-5)
@Service
@RequiredArgsConstructor
public class IssueService {

    private final OrderService orderService;
    private final SupplierClient supplierClient;

    // 결제가 DB에 저장(커밋)된 뒤에, 별도 스레드에서 실행된다
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        if (event.simType() != SimType.ESIM) {
            return;                                   // 유심은 다음 단계에서 처리
        }
        // 1. 주문을 PAID → ISSUING으로 바꾼다 (이미 누가 바꿨으면 그만)
        Optional<String> orderNo = orderService.startIssuing(event.orderId());
        if (orderNo.isEmpty()) {
            return;
        }
        // 2. 공급사에 발급 요청 (DB 트랜잭션 밖)
        Optional<String> supplierOrderNo = supplierClient.requestIssue(orderNo.get());
        // 3. 공급사 접수번호 저장. 실패하면 ISSUING으로 남는다 (다시 요청하는 건 보완 단계)
        supplierOrderNo.ifPresent(no -> orderService.recordSupplierOrderNo(event.orderId(), no));
    }
}
