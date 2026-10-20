package com.roampass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // 대사 스케줄러, 모의 PG 웹훅 예약 전송
public class RoampassApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoampassApplication.class, args);
    }
}
