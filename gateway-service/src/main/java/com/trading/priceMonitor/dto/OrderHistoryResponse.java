package com.trading.priceMonitor.dto;

/**
 * Response DTO for order history queries.
 *
 * <p>Mirrors Order Service's response structure for JSON deserialization.
 */
public record OrderHistoryResponse(
    String orderId,
    String type,
    String region,
    String quantity,
    String price,
    String status,
    String rejectReason,
    String createdAt) {}
