package com.roampass.callback.service;

// 웹훅·콜백 수신 기록의 주인. PG 웹훅과 공급사 콜백이 함께 쓴다 (ARCHITECTURE.md §3-3)
public interface CallbackLogService {

    // 처음 받은 통지면 기록하고 true. 이미 같은 event_key가 있으면 false(중복)
    // 처리하는 쪽 트랜잭션에 합쳐진다. 처리 중 실패하면 이 기록도 같이 롤백되어 다음 재전송이 정상 처리된다
    boolean recordIfNew(String source, String eventKey, String payload);

    void markResult(String eventKey, String result);

    // 처리하지 못한 통지도 남긴다 (CLAUDE.md §3-4). 별도 트랜잭션으로 바로 커밋된다
    void recordRejected(String source, String payload, boolean signatureOk, String result);
}
