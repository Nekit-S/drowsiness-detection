package com.drivermonitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DriverMonitoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriverMonitoringApplication.class, args);
    }
}