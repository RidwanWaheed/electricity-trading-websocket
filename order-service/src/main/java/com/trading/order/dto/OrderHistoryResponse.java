package com.trading.order.dto;

/**
 * Response DTO for order history queries.
 *
 * <p>Contains the fields needed by the frontend to display order history.
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
