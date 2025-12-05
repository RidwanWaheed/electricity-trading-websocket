package com.trading.priceMonitor.config;

import com.trading.priceMonitor.repository.UserRepository;
import com.trading.priceMonitor.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

  private final UserRepository userRepository;
  private final UserService userService;

  public DataInitializer(UserRepository userRepository, UserService userService) {
    this.userRepository = userRepository;
    this.userService = userService;
  }

  @Override
  public void run(String... args) {
    if (userRepository.count() > 0) {
      log.info("Users already exist, skipping data initialization");
      return;
    }

    log.info("Initializing default users...");

    userService.createUser("trader1", "password1");
    userService.createUser("trader2", "password2");
    userService.createUser("admin", "admin123");

    log.info("Default users created successfully");
  }
}
