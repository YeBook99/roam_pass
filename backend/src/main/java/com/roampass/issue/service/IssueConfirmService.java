package com.roampass.issue.service;

import com.roampass.infra.supplier.SupplierIssueCallback;
import com.roampass.issue.domain.IssueConfirmResult;

// 발급 콜백을 우리 DB에 반영한다. 결제 웹훅의 PaymentConfirmService와 같은 모양이다
public interface IssueConfirmService {

    // 중복 기록, 주문 상태 변경, 프로필 저장이 한 트랜잭션이다
    IssueConfirmResult confirm(SupplierIssueCallback callback);
}
