package com.trading.priceMonitor.dto;

public record AuthResult(boolean success, String token, String username, String balance, String error) {

  public static AuthResult success(String token, String username, String balance) {
    return new AuthResult(true, token, username, balance, null);
  }

  public static AuthResult failure(String error) {
    return new AuthResult(false, null, null, null, error);
  }
}
