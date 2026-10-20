package com.roampass.plan.service;

import com.roampass.global.error.BusinessException;
import com.roampass.plan.dto.PlanResponse;
import com.roampass.plan.exception.PlanErrorCode;
import com.roampass.plan.repository.SimPlanRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final SimPlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<PlanResponse> getPlans() {
        return planRepository.findAllOrdered().stream().map(PlanResponse::from).toList();
    }

    // 주문 기능이 주문을 만들 때 가격·SIM 종류를 가져가는 용도 (다른 기능은 이 Service로 조회한다, §4-5)
    @Transactional(readOnly = true)
    public PlanResponse getPlan(Long planId) {
        return planRepository.findById(planId)
                .map(PlanResponse::from)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.PLAN_NOT_FOUND));
    }
}
