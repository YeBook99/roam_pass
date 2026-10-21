package com.roampass.infra.pg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roampass.global.error.BusinessException;
import com.roampass.global.error.CommonErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// PG HTTP 호출은 여기서만 한다. 같은 앱 안의 모의 PG라도 반드시 실제 HTTP로 부른다 (응답 지연·타임아웃 재현, ADR-09)
// 외부 형식 ↔ 우리 결과 변환, 외부 예외 → 우리 예외 변환을 맡고, 재시도는 하지 않는다 (§13)
@Slf4j
@RequiredArgsConstructor
public class HttpPgClient implements PgClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<PgPaymentResult> approve(long paymentId, String orderNo, int amount) {
        try {
            String raw = restClient.post().uri("/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PgApproveRequest(String.valueOf(paymentId), orderNo, amount))
                    .retrieve()
                    .body(String.class);
            return Optional.of(toResult(raw));
        } catch (RestClientException | JsonProcessingException e) {
            // 타임아웃·5xx·읽을 수 없는 응답은 "실패"가 아니라 "모름"이다. 돈이 이미 빠져나갔을 수 있다 (docs/06 §4)
            log.warn("PG 승인 결과를 모름 paymentId={} : {}", paymentId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<PgPaymentResult> find(long paymentId) {
        try {
            String raw = restClient.get().uri("/payments/{id}", paymentId)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            return null;   // PG에 이 요청 기록이 없다
                        }
                        if (response.getStatusCode().isError()) {
                            throw new BusinessException(CommonErrorCode.EXTERNAL_ERROR,
                                    "pg status=" + response.getStatusCode().value());
                        }
                        return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    });
            return raw == null ? Optional.empty() : Optional.of(toResult(raw));
        } catch (ResourceAccessException e) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_UNAVAILABLE, "pg 조회 실패: " + e.getMessage());
        } catch (JsonProcessingException e) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_ERROR, "pg 조회 응답을 읽을 수 없음");
        }
    }

    @Override
    public Optional<PgPaymentResult> parseNotification(byte[] body) {
        try {
            return Optional.of(toResult(new String(body, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private PgPaymentResult toResult(String raw) throws JsonProcessingException {
        PgPaymentMessage m = objectMapper.readValue(raw, PgPaymentMessage.class);
        return new PgPaymentResult(m.merchantRequestId(), m.pgTid(), m.orderNo(), m.amount(), m.status(), raw);
    }

    // PG의 요청·응답 형식. 우리 API DTO를 재사용하지 않는다 (§13-4)
    record PgApproveRequest(String merchantRequestId, String orderNo, int amount) {
    }

    record PgPaymentMessage(String merchantRequestId, String pgTid, String orderNo, int amount, String status) {
    }
}
