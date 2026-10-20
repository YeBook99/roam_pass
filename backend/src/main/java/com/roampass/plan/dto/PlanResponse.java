package com.roampass.plan.dto;

import com.roampass.plan.domain.SimPlan;
import com.roampass.plan.domain.SimType;

public record PlanResponse(Long id, String planCode, String country, int days, int dataMb, SimType simType, int price) {

    public static PlanResponse from(SimPlan plan) {
        return new PlanResponse(plan.getId(), plan.getPlanCode(), plan.getCountry(), plan.getDays(),
                plan.getDataMb(), plan.getSimType(), plan.getPrice());
    }
}
