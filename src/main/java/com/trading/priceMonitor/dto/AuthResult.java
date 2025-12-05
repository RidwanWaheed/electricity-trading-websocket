package com.trading.priceMonitor.dto;

public record AuthResult(boolean success, String token, String username, String error) {

  public static AuthResult success(String token, String username) {
    return new AuthResult(true, token, username, null);
  }

  public static AuthResult failure(String error) {
    return new AuthResult(false, null, null, error);
  }
}
