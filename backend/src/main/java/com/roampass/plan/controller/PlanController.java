package com.roampass.plan.controller;

import com.roampass.plan.dto.PlanResponse;
import com.roampass.plan.service.PlanService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public List<PlanResponse> getPlans() {
        return planService.getPlans();
    }
}
