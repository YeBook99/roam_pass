package com.roampass.plan;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// 요금제. 시드(V2__seed_plans.sql)로만 들어가고 앱에서는 읽기만 한다
@Entity
@Table(name = "sim_plan")
public class SimPlan {

    @Id
    private Long id;

    private String planCode;
    private String country;
    private int days;
    private int dataMb;

    @Enumerated(EnumType.STRING)
    private SimType simType;

    private int price;

    protected SimPlan() {
    }

    public Long getId() {
        return id;
    }

    public String getPlanCode() {
        return planCode;
    }

    public String getCountry() {
        return country;
    }

    public int getDays() {
        return days;
    }

    public int getDataMb() {
        return dataMb;
    }

    public SimType getSimType() {
        return simType;
    }

    public int getPrice() {
        return price;
    }
}
