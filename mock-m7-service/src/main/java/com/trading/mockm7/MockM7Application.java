package com.trading.mockm7;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Mock M7 Service - simulates EPEX SPOT trading engine.
 *
 * <p>EnableScheduling is required for the PricePublisher to broadcast prices on a schedule.
 */
@SpringBootApplication
@EnableScheduling
public class MockM7Application {

  public static void main(String[] args) {
    SpringApplication.run(MockM7Application.class, args);
  }
}
