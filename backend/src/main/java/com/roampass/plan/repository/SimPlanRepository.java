package com.roampass.plan.repository;

import com.roampass.plan.domain.SimPlan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SimPlanRepository extends JpaRepository<SimPlan, Long> {

    @Query("SELECT p FROM SimPlan p ORDER BY p.simType, p.country, p.days")
    List<SimPlan> findAllOrdered();
}
