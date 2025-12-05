package com.trading.priceMonitor.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trading.priceMonitor.dto.OrderMessage;
import com.trading.priceMonitor.entity.OrderEntity;
import com.trading.priceMonitor.entity.UserEntity;
import com.trading.priceMonitor.model.Status;
import com.trading.priceMonitor.repository.OrderRepository;
import com.trading.priceMonitor.repository.UserRepository;

/**
 * Business logic for order processing.
 *
 * This is where the actual work happens:
 * - Validates business rules
 * - Persists orders to database
 * - Updates order status
 *
 * Called by the consumer after receiving messages from the queue.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    /**
     * Processes an order from the message queue.
     *
     * @param message The order message from RabbitMQ
     * @return The processed order entity, or empty if processing failed
     */
    @Transactional
    public Optional<OrderEntity> processOrder(OrderMessage message) {
        log.info("Processing order: orderId={}, user={}",
                message.orderId(), message.username());

        // Check for duplicate order (idempotency)
        if (orderRepository.existsByOrderId(message.orderId())) {
            log.warn("Duplicate order rejected: {}", message.orderId());
            return Optional.empty();
        }

        // Find the user - order must belong to someone
        Optional<UserEntity> userOptional = userRepository.findByUsername(message.username());
        if (userOptional.isEmpty()) {
            log.error("User not found for order: user={}, orderId={}",
                    message.username(), message.orderId());
            return Optional.empty();
        }

        UserEntity user = userOptional.get();

        // Create and save the order
        OrderEntity order = new OrderEntity(
                message.orderId(),
                user,
                message.region(),
                message.orderType(),
                message.quantity(),
                message.price()
        );

        // Business logic: validate and set status
        if (isValidOrder(message)) {
            order.setStatus(Status.ACCEPTED);
        } else {
            order.setStatus(Status.REJECTED);
        }

        OrderEntity savedOrder = orderRepository.save(order);
        log.info("Order processed: orderId={}, status={}",
                savedOrder.getOrderId(), savedOrder.getStatus());

        return Optional.of(savedOrder);
    }

    /**
     * Business validation rules.
     * In a real system, this would check:
     * - User's trading limits
     * - Market hours
     * - Price within acceptable range
     * - Sufficient balance/credit
     */
    private boolean isValidOrder(OrderMessage message) {
        // For learning: simple validation
        // Real trading systems have complex rules here
        return message.quantity().signum() > 0
                && message.price().signum() > 0;
    }

}
