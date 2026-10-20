package com.roampass.payment.domain;

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

// 결제 시도 한 건. 정적 팩토리 대신 PaymentRepository.insertIgnore(네이티브 ON CONFLICT)로만 만든다.
// 같은 Idempotency-Key의 중복 생성을 DB가 막게 하기 위해서다 (CLAUDE.md §3-3). 상태는 조건부 UPDATE로만 바꾼다
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;          // 다른 기능(order)의 Entity는 ID로 참조한다 (§4-8)
    private String idempotencyKey;
    private String pgTid;
    private int amount;            // 주문 금액을 복사해 둔 값. PG 통지 금액과 비교한다
    private int canceledAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private Instant requestedAt;
    private Instant approvedAt;
}
