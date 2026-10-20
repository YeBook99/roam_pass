package com.roampass.plan.domain;

import com.roampass.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 요금제. 시드(V2__seed_plans.sql)로만 들어가고 앱에서는 읽기만 한다
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimPlan extends BaseTimeEntity {

    @Id
    private Long id;

    private String planCode;
    private String country;
    private int days;
    private int dataMb;

    @Enumerated(EnumType.STRING)
    private SimType simType;

    private int price;
}
