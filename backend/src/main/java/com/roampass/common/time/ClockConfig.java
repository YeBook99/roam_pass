package com.roampass.common.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// 현재 시각은 항상 이 Clock으로 얻는다. 테스트에서 시각을 고정할 수 있고, 저장은 UTC로 통일된다
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
