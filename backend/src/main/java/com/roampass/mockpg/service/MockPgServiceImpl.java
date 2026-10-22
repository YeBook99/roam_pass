package com.roampass.mockpg.service;

import com.roampass.global.error.BusinessException;
import com.roampass.mockpg.domain.MockPgCancel;
import com.roampass.mockpg.domain.MockPgPayment;
import com.roampass.mockpg.dto.MockPgApproveRequest;
import com.roampass.mockpg.dto.MockPgCancelRequest;
import com.roampass.mockpg.dto.MockPgCancelResponse;
import com.roampass.mockpg.dto.MockPgPaymentResponse;
import com.roampass.mockpg.dto.MockPgSettings;
import com.roampass.mockpg.dto.MockPgSettingsPatch;
import com.roampass.mockpg.exception.MockPgErrorCode;
import com.roampass.mockpg.repository.MockPgCancelRepository;
import com.roampass.mockpg.repository.MockPgPaymentRepository;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

// MockPgService 구현. 설명은 인터페이스 MockPgService에 있다
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class MockPgServiceImpl implements MockPgService {

    private final MockPgPaymentRepository paymentRepository;
    private final MockPgCancelRepository cancelRepository;
    private final MockPgWebhookSender webhookSender;
    private final TransactionTemplate transactionTemplate;
    private final AtomicReference<MockPgSettings> settings = new AtomicReference<>(MockPgSettings.DEFAULT);

    @Override
    public MockPgSettings getSettings() {
        return settings.get();
    }

    @Override
    public MockPgSettings updateSettings(MockPgSettingsPatch patch) {
        return settings.updateAndGet(s -> s.apply(patch));
    }

    @Override
    public MockPgSettings resetSettings() {
        settings.set(MockPgSettings.DEFAULT);
        return settings.get();
    }

    // 승인을 저장하고 웹훅을 예약한 뒤, 설정만큼 늦게 응답한다
    // 응답 지연이 우리 타임아웃(3초)보다 길면 "PG는 승인했는데 우리는 응답을 못 받은" 상황이 된다
    // 기다리는 동안 DB 커넥션을 잡지 않도록 저장만 TransactionTemplate으로 감싼다
    @Override
    public MockPgPaymentResponse approve(MockPgApproveRequest request) {
        MockPgSettings s = settings.get();
        MockPgPayment payment = transactionTemplate.execute(status ->
                // 같은 merchantRequestId는 기존 결과를 돌려준다 (가맹점의 재요청을 한 번만 승인)
                paymentRepository.findByMerchantRequestId(request.merchantRequestId())
                        .orElseGet(() -> paymentRepository.save(MockPgPayment.create(request.merchantRequestId(),
                                "PG-" + UUID.randomUUID(), request.orderNo(), request.amount(), !s.failApprove()))));
        MockPgPaymentResponse response = MockPgPaymentResponse.from(payment);
        webhookSender.schedule(response, s);
        sleep(s.responseDelayMs());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public MockPgPaymentResponse getPayment(String merchantRequestId) {
        return paymentRepository.findByMerchantRequestId(merchantRequestId)
                .map(MockPgPaymentResponse::from)
                .orElseThrow(() -> new BusinessException(MockPgErrorCode.PAYMENT_NOT_FOUND));
    }

    // 전액·부분 취소. 같은 requestKey는 한 번만 반영한다
    @Override
    @Transactional
    public MockPgCancelResponse cancel(MockPgCancelRequest request) {
        if (ThreadLocalRandom.current().nextDouble() < settings.get().cancelFailRate()) {
            throw new BusinessException(MockPgErrorCode.CANCEL_FAILURE);
        }
        MockPgPayment payment = paymentRepository.findByPgTid(request.pgTid())
                .orElseThrow(() -> new BusinessException(MockPgErrorCode.PAYMENT_NOT_FOUND));
        if (!cancelRepository.existsByRequestKey(request.requestKey())) {
            if (!payment.isCancelable()) {
                throw new BusinessException(MockPgErrorCode.NOT_CANCELABLE);
            }
            if (request.amount() <= 0 || request.amount() > payment.remaining()) {
                throw new BusinessException(MockPgErrorCode.INVALID_CANCEL_AMOUNT);
            }
            payment.cancel(request.amount());
            cancelRepository.save(MockPgCancel.create(request.requestKey(), request.pgTid(), request.amount()));
        }
        return MockPgCancelResponse.from(payment);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
