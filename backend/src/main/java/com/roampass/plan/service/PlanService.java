package com.roampass.plan.service;

import com.roampass.plan.dto.PlanResponse;
import java.util.List;

public interface PlanService {

    List<PlanResponse> getPlans();

    // 주문 기능이 주문을 만들 때 가격·SIM 종류를 가져가는 용도 (다른 기능은 이 Service로 조회한다, §4-5)
    PlanResponse getPlan(Long planId);
}
