package com.roampass.infra.supplier;

import java.util.Optional;

// 공급사 호출. PgClient와 같은 모양이다
public interface SupplierClient {

    // 이심 발급 요청. 공급사는 접수번호(supplierOrderNo)만 바로 주고, ICCID는 나중에 콜백으로 보낸다
    // 실패하거나 응답을 못 받으면 빈 값
    Optional<String> requestIssue(String orderNo);
}
