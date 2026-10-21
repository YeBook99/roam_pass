package com.roampass.history.domain;

// 상태 변경을 알게 된 경로. 같은 결제 승인도 API 응답·웹훅·대사 중 무엇으로 확정됐는지 이력에 남긴다
// EVENT: 커밋 후 이벤트(예: 결제 확정 → 발급 요청)로 시작된 변경
public enum Signal {
    API, WEBHOOK, CALLBACK, POLLING, SCHEDULER, ADMIN, EVENT
}