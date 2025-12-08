package com.trading.priceMonitor.controller;

import com.trading.priceMonitor.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import java.math.BigDecimal;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for user balance queries. */
@RestController
@RequestMapping("/api/balance")
public class BalanceController {

  private final BalanceService balanceService;

  public BalanceController(BalanceService balanceService) {
    this.balanceService = balanceService;
  }

  @Operation(
      summary = "Get balance",
      description = "Get the current EUR balance for the authenticated user")
  @GetMapping
  public ResponseEntity<BalanceResponse> getBalance(Principal principal) {
    String username = principal.getName();
    BigDecimal balance = balanceService.getBalance(username).orElse(BigDecimal.ZERO);
    return ResponseEntity.ok(new BalanceResponse(balance.toString()));
  }

  /** Response DTO for balance query. */
  public record BalanceResponse(String balance) {}
}
