package com.roampass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan   // @ConfigurationProperties record를 빈으로 등록한다 (ARCHITECTURE.md §14-1)
public class RoampassApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoampassApplication.class, args);
    }
}
