package com.roampass.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

// PG 호출. 같은 앱 안의 모의 PG라도 반드시 실제 HTTP로 부른다 (응답 지연·타임아웃을 재현하기 위해, ADR-09)
// 타임아웃·5xx는 RestClientException으로 던진다. 호출한 쪽이 "결과 모름"으로 다룬다
@Component
public class PgClient {

    public record Response(PgResult result, String raw) {
    }

    private final RestClient rest;
    private final ObjectMapper objectMapper;

    public PgClient(@Value("${app.pg.base-url}") String baseUrl, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(3000);   // 타임아웃이 없으면 PG가 답하지 않을 때 스레드가 무한정 묶인다
        this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.objectMapper = objectMapper;
    }

    // merchantRequestId로 우리 paymentId를 보낸다. PG는 같은 ID의 재요청을 한 번만 승인한다
    public Response approve(long paymentId, String orderNo, int amount) {
        String raw = rest.post().uri("/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("merchantRequestId", String.valueOf(paymentId), "orderNo", orderNo, "amount", amount))
                .retrieve()
                .body(String.class);
        return new Response(parse(raw), raw);
    }

    // 대사용 조회. PG에 기록이 없으면(404) empty
    public Optional<Response> find(long paymentId) {
        String raw = rest.get().uri("/payments/{id}", paymentId)
                .exchange((req, res) -> {
                    if (res.getStatusCode().value() == 404) {
                        return null;
                    }
                    if (res.getStatusCode().isError()) {
                        throw new RestClientException("PG 조회 실패: " + res.getStatusCode());
                    }
                    return new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                });
        return Optional.ofNullable(raw).map(r -> new Response(parse(r), r));
    }

    private PgResult parse(String raw) {
        try {
            return objectMapper.readValue(raw, PgResult.class);
        } catch (JsonProcessingException e) {
            throw new RestClientException("PG 응답을 읽을 수 없음: " + raw, e);
        }
    }
}