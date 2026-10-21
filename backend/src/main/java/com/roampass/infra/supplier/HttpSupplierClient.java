package com.roampass.infra.supplier;

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

    // 공급사 응답 형식: { "supplierOrderNo": "..." }
    record IssueResponse(String supplierOrderNo) {
    }
}
