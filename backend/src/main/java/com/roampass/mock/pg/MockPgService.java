package com.roampass.mock.pg;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

// 모의 PG의 승인·취소·조회와 시연 설정
@Service
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class MockPgService {

    public record CancelResult(String pgTid, String status, int canceledAmount) {
    }

    private final MockPgPaymentRepository payments;
    private final MockPgCancelRepository cancels;
    private final Clock clock;
    private final AtomicReference<MockPgSettings> settings = new AtomicReference<>(MockPgSettings.DEFAULT);

    MockPgService(MockPgPaymentRepository payments, MockPgCancelRepository cancels, Clock clock) {
        this.payments = payments;
        this.cancels = cancels;
        this.clock = clock;
    }

    MockPgSettings settings() {
        return settings.get();
    }

    MockPgSettings update(MockPgSettings.Patch patch) {
        return settings.updateAndGet(s -> s.apply(patch));
    }

    void reset() {
        settings.set(MockPgSettings.DEFAULT);
    }

    // 같은 merchantRequestId는 기존 결과를 돌려준다 (가맹점의 재요청을 한 번만 승인)
    @Transactional
    public MockPgResult approve(String merchantRequestId, String orderNo, int amount) {
        return payments.findByMerchantRequestId(merchantRequestId)
                .orElseGet(() -> payments.save(new MockPgPayment(merchantRequestId, "PG-" + UUID.randomUUID(), orderNo, amount,
                        settings().failApprove() ? "FAILED" : "APPROVED", clock.instant())))
                .toResult();
    }

    @Transactional(readOnly = true)
    public Optional<MockPgResult> find(String merchantRequestId) {
        return payments.findByMerchantRequestId(merchantRequestId).map(MockPgPayment::toResult);
    }

    // 전액·부분 취소. 같은 requestKey는 한 번만 반영한다
    @Transactional
    public CancelResult cancel(String pgTid, String requestKey, int amount) {
        if (ThreadLocalRandom.current().nextDouble() < settings().cancelFailRate()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "모의 PG 취소 장애");
        }
        MockPgPayment p = payments.findByPgTid(pgTid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "거래 없음"));
        boolean alreadyDone = cancels.findByRequestKey(requestKey).isPresent();
        if (!alreadyDone) {
            if (!p.getStatus().equals("APPROVED") && !p.getStatus().equals("PARTIAL_CANCELED")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "취소할 수 없는 상태: " + p.getStatus());
            }
            if (amount <= 0 || amount > p.remaining()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "취소 금액 오류");
            }
            p.cancel(amount);
            cancels.save(new MockPgCancel(requestKey, pgTid, amount, clock.instant()));
        }
        return new CancelResult(p.getPgTid(), p.getStatus(), p.getCanceledAmount());
    }
}