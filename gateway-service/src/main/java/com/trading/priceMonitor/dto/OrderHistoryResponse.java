package com.trading.priceMonitor.dto;

import com.trading.common.Region;

/**
 * Response DTO for order history queries.
 *
 * <p>Mirrors Order Service's response structure for JSON deserialization.
 */
public record OrderHistoryResponse(
    String orderId,
    String type,
    Region region,
    String quantity,
    String price,
    String status,
    String rejectReason,
    String createdAt) {}
