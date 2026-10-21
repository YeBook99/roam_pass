package com.roampass.issue.domain;

import com.roampass.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 발급 결과(이심 프로필). 행은 SimProfileRepository.insertIgnore(ON CONFLICT)로만 만든다 (중복 콜백을 DB가 막게)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private String iccid;
    private String activationCode;
    private String supplierOrderNo;

    @Enumerated(EnumType.STRING)
    private InstallStatus installStatus;
}
