package com.trading.priceMonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Enables scheduled tasks for price broadcasting
public class ElectricityPriceMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElectricityPriceMonitorApplication.class, args);
    }
}
