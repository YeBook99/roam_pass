package com.roampass.infra.supplier;

// 공급사 발급 콜백을 우리 쪽 형태로 바꾼 것. rawPayload = 받은 원문 (callback_log에 그대로 남긴다)
public record SupplierIssueCallback(String supplierOrderNo, String orderNo, String iccid, String activationCode,
                                    String rawPayload) {
}
