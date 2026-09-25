package com.influencerapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
/**
 * Spring Boot application entry point for the InfluencerAPP service.
 *
 * @author judaro122
 * @since 1.0.0
 */


public class InfluencerAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfluencerAppApplication.class, args);
    }
}
