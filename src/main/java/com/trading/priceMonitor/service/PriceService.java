package com.trading.priceMonitor.service;

import com.trading.priceMonitor.model.ElectricityPrice;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PriceService {

  private final SimpMessagingTemplate messagingTemplate;
  private final Random random = new Random();
  private final List<String> regions = List.of("NORTH", "SOUTH", "EAST", "WEST");

  private final Map<String, BigDecimal> previousPrices = new HashMap<>();

  public PriceService(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @Scheduled(fixedRate = 2000)
  public void broadcastPrices() {
    // Generate and send prices for each region
    for (String region : regions) {
      double priceDouble = 50 + (150 - 50) * random.nextDouble(); // Random price between 50 and 150
      BigDecimal newPrice = BigDecimal.valueOf(priceDouble).setScale(2, RoundingMode.HALF_UP);

      BigDecimal previousPrice = previousPrices.getOrDefault(region, BigDecimal.ZERO);

      double changePercent = 0.0;
      if (previousPrice.compareTo(BigDecimal.ZERO) != 0) {
        changePercent =
            newPrice
                .subtract(previousPrice)
                .divide(previousPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
      }

      ElectricityPrice priceUpdate =
          new ElectricityPrice("Nigeria", region, newPrice, "NGN", changePercent, Instant.now());

      messagingTemplate.convertAndSend("/topic/prices", priceUpdate);

      previousPrices.put(region, newPrice);
    }
  }
}
