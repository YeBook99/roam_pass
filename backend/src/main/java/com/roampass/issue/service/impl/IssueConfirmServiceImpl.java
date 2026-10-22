package com.roampass.issue.service.impl;

import com.roampass.callback.service.CallbackLogService;
import com.roampass.infra.supplier.SupplierIssueCallback;
import com.roampass.issue.domain.IssueConfirmResult;
import com.roampass.issue.repository.SimProfileRepository;
import com.roampass.issue.service.IssueConfirmService;
import com.roampass.order.service.OrderService;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// IssueConfirmService 구현. 설명은 인터페이스 IssueConfirmService에 있다
@Service
@RequiredArgsConstructor
public class IssueConfirmServiceImpl implements IssueConfirmService {

    private final CallbackLogService callbackLogService;
    private final OrderService orderService;
    private final SimProfileRepository profileRepository;
    private final Clock clock;

    // 중복 기록, 주문 상태 변경, 프로필 저장이 한 트랜잭션이다
    @Override
    @Transactional
    public IssueConfirmResult confirm(SupplierIssueCallback callback) {
        // 1. 중복 체크: 같은 공급사 주문의 발급 완료는 한 번만 기록된다
        String eventKey = "SUP:" + callback.supplierOrderNo() + ":ISSUED";
        if (!callbackLogService.recordIfNew("SUPPLIER", eventKey, callback.rawPayload())) {
            return IssueConfirmResult.DUPLICATE;
        }

        // 2. 어느 주문 것인지 찾는다
        Optional<Long> orderId = orderService.findOrderIdForSupplier(callback.supplierOrderNo(), callback.orderNo());
        if (orderId.isEmpty()) {
            return mark(eventKey, IssueConfirmResult.UNMATCHED);
        }

        // 3. 주문 ISSUING → ISSUED (이미 바뀌었으면 그만)
        if (!orderService.markIssued(orderId.get())) {
            return mark(eventKey, IssueConfirmResult.STALE_IGNORED);
        }

        // 4. 프로필 저장. order_id·iccid UNIQUE라 같은 프로필이 두 번 생기지 않는다
        int inserted = profileRepository.insertIgnore(orderId.get(), callback.iccid(), callback.activationCode(),
                callback.supplierOrderNo(), Instant.now(clock));
        if (inserted == 0) {
            // ISSUED인데 프로필이 없는 상태를 만들지 않도록 전체를 롤백한다 (예: 같은 ICCID가 다른 주문에 이미 있음)
            throw new IllegalStateException("프로필을 저장하지 못함 orderId=" + orderId.get() + ", iccid=" + callback.iccid());
        }
        return mark(eventKey, IssueConfirmResult.PROCESSED);
    }

    private IssueConfirmResult mark(String eventKey, IssueConfirmResult result) {
        callbackLogService.markResult(eventKey, result.name());
        return result;
    }
}
