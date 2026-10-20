package com.roampass.mockpg.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roampass.callback.domain.Hmac;
import com.roampass.mockpg.dto.MockPgPaymentResponse;
import com.roampass.mockpg.dto.MockPgSettings;
import java.time.Clock;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// 승인 결과를 우리 서버로 통지(웹훅)한다. 설정에 따라 지연·누락·중복·금액 조작을 섞는다
// 모의 PG는 "외부 시스템 자신"이라 infra를 거치지 않고 여기서 직접 HTTP를 보낸다 (ADR-15의 예외)
@Slf4j
@Component
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class MockPgWebhookSender {

    private final TaskScheduler scheduler;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final RestClient restClient;
    private final String secret;

    public MockPgWebhookSender(TaskScheduler scheduler, ObjectMapper objectMapper, Clock clock,
                               RestClient.Builder builder, MockPgProperties properties) {
        this.scheduler = scheduler;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.restClient = builder.clone().baseUrl(properties.callbackBaseUrl()).build();
        this.secret = properties.webhookSecret();
    }

    public void schedule(MockPgPaymentResponse result, MockPgSettings settings) {
        MockPgPaymentResponse body = result.withAmountDelta(settings.webhookAmountDelta());
        Instant sendAt = Instant.now(clock).plusMillis(settings.webhookDelayMs());
        for (int i = 0; i < settings.webhookCount(); i++) {
            scheduler.schedule(() -> send(body), sendAt);
        }
    }

    private void send(MockPgPaymentResponse body) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(body);
            restClient.post().uri("/api/webhooks/pg")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Signature", Hmac.sign(json, secret))
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();
        } catch (JsonProcessingException | RestClientException e) {
            log.warn("모의 PG 웹훅 전송 실패 pgTid={} : {}", body.pgTid(), e.getMessage());
        }
    }
}
