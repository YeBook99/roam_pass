package com.roampass.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

// 통합 테스트 공통 부모. 운영과 같은 DB를 컨테이너로 한 번만 띄워 모든 통합 테스트가 같이 쓴다 (ARCHITECTURE.md §16)
// Docker가 필요하다
@SpringBootTest(properties = {
        "PG_WEBHOOK_SECRET=test-secret",
        "external.pg.base-url=http://localhost:1",   // 테스트가 로컬에서 떠 있는 서버(8080)의 모의 PG를 실수로 부르지 않게
        "app.payment.reconcile-delay-ms=3600000",   // 테스트 중에 대사 스케줄러가 끼어들지 않게
})
public abstract class IntegrationTestSupport {

    @ServiceConnection   // 컨테이너 주소·계정을 datasource 설정에 자동으로 넣어 준다
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
