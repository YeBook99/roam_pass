package com.roampass.history.domain;

import com.roampass.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 상태 변경 이력 한 줄. 어드민 주문 상세 타임라인의 원천이다. createdAt = 우리가 기록한 시각
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusHistory extends BaseTimeEntity {

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

    public static StatusHistory of(TargetType targetType, Long targetId, String fromStatus, String toStatus,
                                   Actor actor, Signal signal, String reason, Instant occurredAt) {
        StatusHistory history = new StatusHistory();
        history.targetType = targetType;
        history.targetId = targetId;
        history.fromStatus = fromStatus;
        history.toStatus = toStatus;
        history.actor = actor.value();
        history.signal = signal;
        history.reason = reason;
        history.occurredAt = occurredAt;
        return history;
    }
}