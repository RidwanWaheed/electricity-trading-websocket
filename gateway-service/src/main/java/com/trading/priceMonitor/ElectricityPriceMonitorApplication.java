package com.trading.priceMonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gateway Service - entry point for the trading platform.
 *
 * <p>Handles WebSocket connections, REST API, authentication, and routes messages between clients
 * and backend services via RabbitMQ.
 *
 * <p>Gateway receives prices via RabbitMQ and forwards to WebSocket clients.
 */
@SpringBootApplication
public class ElectricityPriceMonitorApplication {

  public static void main(String[] args) {
    SpringApplication.run(ElectricityPriceMonitorApplication.class, args);
  }
}
