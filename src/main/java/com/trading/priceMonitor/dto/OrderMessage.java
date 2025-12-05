package com.trading.priceMonitor.dto;

import java.math.BigDecimal;

/**
 * Internal message for RabbitMQ queue.
 * Username is added by server from authenticated Principal (not from client request).
 */
public record OrderMessage(
        String orderId,
        String username,
        String region,
        String orderType,
        BigDecimal quantity,
        BigDecimal price
) {}
