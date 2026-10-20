package com.roampass.common.callback;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// 받은 웹훅·콜백 기록. 중복 방지(event_key UNIQUE). 서명 검증/누락 확인
@Entity
@Table(name = "callback_log")
public class CallbackLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;
    private String eventKey;
    private String payload;
    private boolean signatureOk;
    private String result;
    private Instant receivedAt;

    protected CallbackLog() {
    }

    // 서명이 틀린 통지. event_key를 비워 둬서, 위조 요청이 진짜 통지의 키를 먼저 차지하지 못하게 한다
    public static CallbackLog invalidSignature(String source, String payload, Instant receivedAt) {
        CallbackLog log = new CallbackLog();
        log.source = source;
        log.payload = payload;
        log.signatureOk = false;
        log.result = "SIGNATURE_INVALID";
        log.receivedAt = receivedAt;
        return log;
    }
}