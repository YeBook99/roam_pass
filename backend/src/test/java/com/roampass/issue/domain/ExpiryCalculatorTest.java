package com.roampass.issue.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.roampass.global.config.ClockConfig;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 과제 권장 테스트(2.5-5): 만료일 UTC → KST 계산. 과제 부록 A의 예시를 그대로 검증한다
class ExpiryCalculatorTest {

    @Test
    @DisplayName("2026-05-01T15:20Z에 개통한 7일 요금제는 KST 2026-05-09 00:20에 만료된다")
    void 부록_예시_UTC_개통_7일_KST_만료() {
        Instant activatedAt = Instant.parse("2026-05-01T15:20:00Z");   // KST로는 이미 5월 2일 00:20

        Instant expiresAt = ExpiryCalculator.expiresAt(activatedAt, 7);

        assertThat(expiresAt).isEqualTo(Instant.parse("2026-05-08T15:20:00Z"));   // 정확히 168시간 뒤 (UTC)
        assertThat(expiresAt.atZone(ClockConfig.KST).toLocalDateTime())
                .isEqualTo(LocalDateTime.of(2026, 5, 9, 0, 20));                   // KST로 보면 5월 9일 00:20
    }
}
