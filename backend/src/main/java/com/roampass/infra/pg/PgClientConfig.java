package com.roampass.infra.pg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// 타임아웃은 반드시 설정한다. 기본값(무한 대기)이면 PG 장애가 우리 스레드를 모두 잡는다 (ARCHITECTURE.md §13-3)
@Configuration
public class PgClientConfig {

    @Bean
    public PgClient pgClient(RestClient.Builder builder, PgProperties properties, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        RestClient restClient = builder.clone()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
        return new HttpPgClient(restClient, objectMapper);
    }
}
