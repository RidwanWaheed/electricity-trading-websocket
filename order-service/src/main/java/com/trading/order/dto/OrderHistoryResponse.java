package com.trading.order.dto;

import com.trading.common.Region;

/**
 * Response DTO for order history queries.
 *
 * <p>Contains the fields needed by the frontend to display order history.
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
