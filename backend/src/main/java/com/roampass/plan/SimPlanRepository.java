package com.roampass.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimPlanRepository extends JpaRepository<SimPlan, Long> {

    List<SimPlan> findAllByOrderBySimTypeAscCountryAscDaysAsc();
}
