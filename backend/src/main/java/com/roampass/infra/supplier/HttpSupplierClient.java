package com.roampass.infra.supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// 공급사 HTTP 호출. 같은 앱 안의 모의 공급사라도 실제 HTTP로 부른다 (ADR-09)
@Slf4j
@RequiredArgsConstructor
public class HttpSupplierClient implements SupplierClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<String> requestIssue(String orderNo) {
        try {
            IssueResponse response = restClient.post().uri("/orders")
                    .body(Map.of("orderNo", orderNo))
                    .retrieve()
                    .body(IssueResponse.class);
            return Optional.ofNullable(response).map(IssueResponse::supplierOrderNo);
        } catch (RestClientException e) {
            log.warn("공급사 발급 요청 실패 orderNo={} : {}", orderNo, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<SupplierIssueCallback> parseIssueCallback(byte[] body) {
        try {
            String raw = new String(body, StandardCharsets.UTF_8);
            IssueCallbackMessage m = objectMapper.readValue(body, IssueCallbackMessage.class);
            if (m.supplierOrderNo() == null || m.orderNo() == null || m.iccid() == null) {
                return Optional.empty();   // 필수 값이 빠진 콜백은 읽을 수 없는 본문으로 본다
            }
            return Optional.of(new SupplierIssueCallback(m.supplierOrderNo(), m.orderNo(), m.iccid(), m.activationCode(), raw));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<SupplierProfileState> getProfile(String iccid) {
        try {
            ProfileResponse response = restClient.get().uri("/profiles/{iccid}", iccid)
                    .retrieve()
                    .body(ProfileResponse.class);
            return Optional.ofNullable(response).map(r -> new SupplierProfileState(r.state(), r.activatedAt()));
        } catch (RestClientException e) {
            log.warn("공급사 프로필 조회 실패 iccid={} : {}", iccid, e.getMessage());
            return Optional.empty();
        }
    }

    // 공급사 프로필 조회 형식: { "iccid", "state", "activatedAt" }
    record ProfileResponse(String iccid, String state, Instant activatedAt) {
    }

    // 공급사 응답 형식: { "supplierOrderNo": "..." }
    record IssueResponse(String supplierOrderNo) {
    }

    // 공급사 콜백 형식: { "supplierOrderNo", "orderNo", "iccid", "activationCode" }
    record IssueCallbackMessage(String supplierOrderNo, String orderNo, String iccid, String activationCode) {
    }
}
