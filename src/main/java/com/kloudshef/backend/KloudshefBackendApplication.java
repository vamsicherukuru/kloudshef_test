package com.kloudshef.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KloudshefBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(KloudshefBackendApplication.class, args);
    }
}
