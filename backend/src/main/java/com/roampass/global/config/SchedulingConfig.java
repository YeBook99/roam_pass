package com.roampass.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// @Scheduled(대사 스케줄러)와 TaskScheduler(모의 PG 웹훅 예약 전송)를 켠다
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
