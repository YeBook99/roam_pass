package com.roampass.mocksupplier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roampass.callback.domain.Hmac;
import com.roampass.mocksupplier.domain.MockSupplierOrder;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// 발급 결과(ICCID·활성화 코드)를 몇 초 뒤 우리 서버로 콜백한다. 모의 PG 웹훅 발송과 같은 모양이다
@Slf4j
@Component
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class MockSupplierCallbackSender {

    private final TaskScheduler scheduler;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final RestClient restClient;
    private final MockSupplierProperties properties;

    public MockSupplierCallbackSender(TaskScheduler scheduler, ObjectMapper objectMapper, Clock clock,
                                      RestClient.Builder builder, MockSupplierProperties properties) {
        this.scheduler = scheduler;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.restClient = builder.clone().baseUrl(properties.callbackBaseUrl()).build();
        this.properties = properties;
    }

    public void schedule(MockSupplierOrder order) {
        Map<String, String> body = Map.of(
                "supplierOrderNo", order.getSupplierOrderNo(),
                "orderNo", order.getOrderNo(),
                "iccid", order.getIccid(),
                "activationCode", order.getActivationCode());
        scheduler.schedule(() -> send(body), Instant.now(clock).plusMillis(properties.callbackDelayMs()));
    }

    private void send(Map<String, String> body) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(body);
            restClient.post().uri("/api/webhooks/supplier")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Signature", Hmac.sign(json, properties.callbackSecret()))
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();
        } catch (JsonProcessingException | RestClientException e) {
            log.warn("모의 공급사 콜백 전송 실패 orderNo={} : {}", body.get("orderNo"), e.getMessage());
        }
    }
}
