package com.roampass.mock.pg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roampass.common.callback.Hmac;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;

// 승인 결과를 우리 서버로 통지(웹훅)한다. 설정에 따라 지연·누락·중복·금액 조작을 섞는다
@Component
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
class MockPgWebhookSender {

    private static final Logger log = LoggerFactory.getLogger(MockPgWebhookSender.class);

    private final TaskScheduler scheduler;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final RestClient rest;
    private final String secret;

    MockPgWebhookSender(TaskScheduler scheduler, ObjectMapper objectMapper, Clock clock,
                        @Value("${app.public-base-url}") String baseUrl, @Value("${mock.pg.webhook-secret}") String secret) {
        this.scheduler = scheduler;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
        this.secret = secret;
    }

    void schedule(MockPgResult result, MockPgSettings s) {
        MockPgResult body = new MockPgResult(result.merchantRequestId(), result.pgTid(), result.orderNo(),
                result.amount() + s.webhookAmountDelta(), result.status());
        for (int i = 0; i < s.webhookCount(); i++) {
            scheduler.schedule(() -> send(body), clock.instant().plusMillis(s.webhookDelayMs()));
        }
    }

    private void send(MockPgResult body) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(body);
            rest.post().uri("/api/webhooks/pg")
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