package com.roampass.common.status;

// 상태 변경을 알게 된 경로. 같은 결제 승인도 API 응답·웹훅·대사 중 무엇으로 확정됐는지 이력에 남긴다
public enum Signal {
    API, WEBHOOK, CALLBACK, POLLING, SCHEDULER, ADMIN
}
