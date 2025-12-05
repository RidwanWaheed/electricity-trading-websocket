package com.trading.priceMonitor.dto;

import java.math.BigDecimal;

/**
 * Message sent through RabbitMQ containing order details and user context.
 *
 * Why a separate DTO from Order?
 * - Order is what the client sends (client shouldn't set username - security risk)
 * - OrderMessage is what flows through the queue (includes server-validated username)
 * - Decouples WebSocket API contract from internal messaging contract
 */
public record OrderMessage(
        String orderId,
        String username,      // Added by server, not from client
        String region,
        String orderType,
        BigDecimal quantity,
        BigDecimal price
) {}
