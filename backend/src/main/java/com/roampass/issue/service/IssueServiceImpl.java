package com.roampass.issue.service;

import com.roampass.callback.domain.Hmac;
import com.roampass.callback.service.CallbackLogService;
import com.roampass.global.error.BusinessException;
import com.roampass.history.domain.Signal;
import com.roampass.infra.supplier.SupplierClient;
import com.roampass.infra.supplier.SupplierIssueCallback;
import com.roampass.infra.supplier.SupplierProperties;
import com.roampass.issue.dto.ProfileResponse;
import com.roampass.issue.exception.IssueErrorCode;
import com.roampass.issue.repository.SimProfileRepository;
import com.roampass.order.domain.OrderPaidEvent;
import com.roampass.order.service.OrderService;
import com.roampass.plan.domain.SimType;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// IssueService 구현. 설명은 인터페이스 IssueService에 있다
@Service
@RequiredArgsConstructor
public class IssueServiceImpl implements IssueService {

    private final OrderService orderService;
    private final SupplierClient supplierClient;
    private final SupplierProperties supplierProperties;
    private final IssueConfirmService confirmService;
    private final CallbackLogService callbackLogService;
    private final SimProfileRepository profileRepository;

    // 결제가 DB에 저장(커밋)된 뒤에, 별도 스레드에서 실행된다
    // 서버가 그 순간 죽으면 이 이벤트가 사라질 수 있어서, IssueScheduler가 PAID로 남은 주문을 다시 집어 간다
    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        handlePaidOrder(event.orderId(), event.simType(), Signal.EVENT);
    }

    // 결제된 주문 처리. 이벤트(EVENT)와 안전망 스케줄러(SCHEDULER)가 같이 쓴다. signal은 이력에 누가 시작했는지 남기는 용도
    // 둘이 동시에 불러도 주문 상태 조건부 UPDATE 때문에 한 번만 진행된다
    @Override
    public void handlePaidOrder(Long orderId, SimType simType, Signal signal) {
        if (simType == SimType.USIM) {
            orderService.markPickupReady(orderId, signal);    // 유심은 공항에서 카드를 받으므로 공급사 발급 대신 수령 대기
            return;
        }
        // 이심: 1. 주문을 PAID → ISSUING으로 바꾼다 (이미 누가 바꿨으면 그만)
        Optional<String> orderNo = orderService.startIssuing(orderId, signal);
        if (orderNo.isEmpty()) {
            return;
        }
        // 2. 공급사에 발급 요청 (DB 트랜잭션 밖)
        Optional<String> supplierOrderNo = supplierClient.requestIssue(orderNo.get());
        // 3. 공급사 접수번호 저장. 실패하면 ISSUING으로 남는다 (다시 요청하는 건 보완 단계)
        supplierOrderNo.ifPresent(no -> orderService.recordSupplierOrderNo(orderId, no));
    }

    // 내 주문의 발급 결과. 남의 주문은 OrderService가 404로 막는다
    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(String orderNo, String userId) {
        Long orderId = orderService.getMyOrderId(orderNo, userId);
        return profileRepository.findByOrderId(orderId)
                .map(profile -> ProfileResponse.from(orderNo, profile))
                .orElseThrow(() -> new BusinessException(IssueErrorCode.PROFILE_NOT_FOUND));
    }

    // 공급사 발급 콜백. 결제 웹훅(PaymentService.receiveWebhook)과 같은 순서다
    // 서명 검증 → 본문 읽기 → 반영. 처리하지 못한 콜백도 callback_log에 남긴다 (CLAUDE.md §3-4)
    @Override
    public void receiveCallback(byte[] body, String signature) {
        String raw = new String(body, StandardCharsets.UTF_8);
        if (!Hmac.verify(body, signature, supplierProperties.callbackSecret())) {
            callbackLogService.recordRejected("SUPPLIER", raw, false, "SIGNATURE_INVALID");
            throw new BusinessException(IssueErrorCode.INVALID_CALLBACK_SIGNATURE);
        }
        Optional<SupplierIssueCallback> callback = supplierClient.parseIssueCallback(body);
        if (callback.isEmpty()) {
            callbackLogService.recordRejected("SUPPLIER", raw, true, "INVALID_BODY");
            throw new BusinessException(IssueErrorCode.INVALID_CALLBACK_BODY);
        }
        confirmService.confirm(callback.get());
    }
}
