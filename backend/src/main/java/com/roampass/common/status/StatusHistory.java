package com.roampass.common.status;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// 상태 변경 이력 한 줄. 어드민 주문 상세 타임라인의 원천이다
@Entity
@Table(name = "status_history")
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    private Long targetId;
    private String fromStatus;
    private String toStatus;
    private String actor;

    @Enumerated(EnumType.STRING)
    private Signal signal;

    private String reason;
    private Instant occurredAt;   // 신호가 말하는 발생 시각
    private Instant recordedAt;   // 우리가 기록한 시각

    protected StatusHistory() {
    }

    public StatusHistory(TargetType targetType, Long targetId, String fromStatus, String toStatus,
                         Actor actor, Signal signal, String reason, Instant occurredAt, Instant recordedAt) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor.value();
        this.signal = signal;
        this.reason = reason;
        this.occurredAt = occurredAt;
        this.recordedAt = recordedAt;
    }
}
