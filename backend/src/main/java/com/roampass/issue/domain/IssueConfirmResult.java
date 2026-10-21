package com.roampass.issue.domain;

// 발급 콜백을 반영한 결과. 이 이름이 callback_log.result에 그대로 남는다
public enum IssueConfirmResult {
    PROCESSED,       // 처리함
    DUPLICATE,       // 같은 콜백이 이미 처리됨
    UNMATCHED,       // 우리 주문을 찾을 수 없음
    STALE_IGNORED    // 주문이 이미 ISSUING이 아님
}
