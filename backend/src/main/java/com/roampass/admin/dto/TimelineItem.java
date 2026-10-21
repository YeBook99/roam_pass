package com.roampass.admin.dto;

import java.time.Instant;

// 타임라인 한 줄: 언제(occurredAt 발생, recordedAt 기록), 무엇이(target from→to), 누가/어떤 신호로(actor, signal)
public record TimelineItem(String target, String fromStatus, String toStatus, String actor, String signal,
                           String reason, Instant occurredAt, Instant recordedAt) {
}
