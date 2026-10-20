package com.roampass.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 현재 시각은 이 Clock으로만 얻는다. 테스트에서 고정 Clock으로 바꿀 수 있다 (ARCHITECTURE.md §14-3)
@Configuration
public class ClockConfig {

    // 운영 기준 시간대. 저장은 UTC로 하고, 날짜 판단과 화면 표시에만 쓴다 (CLAUDE.md §3-7)
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
