package com.vgc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VgcApplication {
    public static void main(String[] args) {
        SpringApplication.run(VgcApplication.class, args);
    }
}
