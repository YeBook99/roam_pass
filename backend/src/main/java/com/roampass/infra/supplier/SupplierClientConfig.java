package com.roampass.infra.supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// PgClientConfig와 같은 모양. 타임아웃을 꼭 건다 (없으면 공급사가 응답하지 않을 때 스레드가 무한정 묶인다)
@Configuration
public class SupplierClientConfig {

    @Bean
    public SupplierClient supplierClient(RestClient.Builder builder, SupplierProperties properties,
                                         ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        RestClient restClient = builder.clone()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
        return new HttpSupplierClient(restClient, objectMapper);
    }
}
