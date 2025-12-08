package com.trading.common;

/** Trading regions supported by the platform. */
public enum Region {
  NORTH,
  SOUTH,
  EAST,
  WEST;

  /** Parse string to Region (case-insensitive). */
  public static Region fromString(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Region cannot be null or blank");
    }

    String normalized = value.trim().toUpperCase();

    try {
      return Region.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid region: '" + value + "'. Valid regions are: NORTH, SOUTH, EAST, WEST");
    }
  }

  /** Check if a string is a valid region. */
  public static boolean isValid(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }

    try {
      Region.valueOf(value.trim().toUpperCase());
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
