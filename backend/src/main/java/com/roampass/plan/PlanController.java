package com.roampass.plan;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 요금제 목록은 단순 조회라 서비스 계층 없이 리포지토리를 바로 쓴다
@RestController
public class PlanController {

    private final SimPlanRepository planRepository;

    public PlanController(SimPlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public record PlanResponse(Long id, String planCode, String country, int days, int dataMb, SimType simType, int price) {
        static PlanResponse of(SimPlan p) {
            return new PlanResponse(p.getId(), p.getPlanCode(), p.getCountry(), p.getDays(), p.getDataMb(), p.getSimType(), p.getPrice());
        }
    }

    @GetMapping("/api/plans")
    public List<PlanResponse> plans() {
        return planRepository.findAllByOrderBySimTypeAscCountryAscDaysAsc().stream().map(PlanResponse::of).toList();
    }
}
