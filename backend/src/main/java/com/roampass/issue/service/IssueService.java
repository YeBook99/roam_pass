package com.roampass.issue.service;

import com.roampass.history.domain.Signal;
import com.roampass.issue.dto.ProfileResponse;
import com.roampass.order.domain.OrderPaidEvent;
import com.roampass.plan.domain.SimType;

// 발급. 결제가 끝나면 서버가 알아서 발급을 시작한다 (클라이언트가 부르지 않는다, CLAUDE.md §3-5)
public interface IssueService {

    // 결제가 DB에 저장(커밋)된 뒤에, 별도 스레드에서 실행된다
    // 서버가 그 순간 죽으면 이 이벤트가 사라질 수 있어서, IssueScheduler가 PAID로 남은 주문을 다시 집어 간다
    void onOrderPaid(OrderPaidEvent event);

    // 결제된 주문 처리. 이벤트(EVENT)와 안전망 스케줄러(SCHEDULER)가 같이 쓴다. signal은 이력에 누가 시작했는지 남기는 용도
    // 둘이 동시에 불러도 주문 상태 조건부 UPDATE 때문에 한 번만 진행된다
    void handlePaidOrder(Long orderId, SimType simType, Signal signal);

    // 내 주문의 발급 결과. 남의 주문은 OrderService가 404로 막는다
    ProfileResponse getMyProfile(String orderNo, String userId);

    // 공급사 발급 콜백. 결제 웹훅(PaymentService.receiveWebhook)과 같은 순서다
    // 서명 검증 → 본문 읽기 → 반영. 처리하지 못한 콜백도 callback_log에 남긴다 (CLAUDE.md §3-4)
    void receiveCallback(byte[] body, String signature);
}
