package com.trading.priceMonitor.controller;

import com.trading.priceMonitor.service.BalanceService;
import java.math.BigDecimal;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user balance queries.
 *
 * <p>Provides endpoint for frontend to fetch current user balance.
 */
@RestController
@RequestMapping("/api/balance")
public class BalanceController {

  private final BalanceService balanceService;

  public BalanceController(BalanceService balanceService) {
    this.balanceService = balanceService;
  }

  /**
   * Get the current balance for the authenticated user.
   *
   * @param principal The authenticated user
   * @return The balance in EUR
   */
  @GetMapping
  public ResponseEntity<BalanceResponse> getBalance(Principal principal) {
    String username = principal.getName();
    BigDecimal balance = balanceService.getBalance(username).orElse(BigDecimal.ZERO);
    return ResponseEntity.ok(new BalanceResponse(balance.toString()));
  }

  /** Response DTO for balance query. */
  public record BalanceResponse(String balance) {}
}
