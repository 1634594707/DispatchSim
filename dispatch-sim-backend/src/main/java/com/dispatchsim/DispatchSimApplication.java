package com.dispatchsim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DispatchSimApplication {

    public static void main(String[] args) {
        SpringApplication.run(DispatchSimApplication.class, args);
    }
}
