package com.roampass.callback.domain;

import com.roampass.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 받은 웹훅·콜백 기록. 중복 방지(event_key UNIQUE). 서명 검증/누락 확인. createdAt = 받은 시각
// 처리한 통지는 CallbackLogRepository.insertIgnore(네이티브)로 만들고, 처리하지 못한 통지만 이 팩토리로 만든다
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CallbackLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;
    private String eventKey;
    private String payload;
    private boolean signatureOk;
    private String result;

    // 처리하지 못한 통지(서명 오류, 본문 오류). event_key를 비워 둬서 위조 요청이 진짜 통지의 키를 먼저 차지하지 못하게 한다
    public static CallbackLog rejected(String source, String payload, boolean signatureOk, String result) {
        CallbackLog log = new CallbackLog();
        log.source = source;
        log.payload = payload;
        log.signatureOk = signatureOk;
        log.result = result;
        return log;
    }
}