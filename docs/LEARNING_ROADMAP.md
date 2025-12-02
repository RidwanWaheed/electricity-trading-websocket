# WebSocket Learning Roadmap

A structured guide for learning WebSocket with Spring Boot for electricity trading software.

## Completed Topics

### Part 1: Server → Client Broadcasting
- [x] WebSocket configuration with STOMP
- [x] Message broker setup (`/topic`, `/queue`)
- [x] Scheduled price broadcasting
- [x] Client → Server messaging (`@MessageMapping`)

### Part 2: User-Specific Messages
- [x] User destination prefix (`/user`)
- [x] `SimpMessagingTemplate.convertAndSendToUser()`
- [x] Private queues for order confirmations

### Part 3: Error Handling
- [x] `@MessageExceptionHandler`
- [x] `WebSocketEventListener` for connection events
- [x] Error queues (`/queue/errors`)

### Part 4: JWT Authentication
- [x] `JwtService` - Token generation & validation
- [x] `AuthController` - REST login endpoint
- [x] `JwtHandshakeInterceptor` - Validates JWT during handshake
- [x] `UserInterceptor` - Sets Principal for Spring
- [x] `SecurityConfig` - HTTP security configuration

---

## Upcoming Topics

### Part 5: Heartbeat & Connection Management
- Configure STOMP heartbeats to detect dead connections
- Handle connection timeouts gracefully
- Server-side connection tracking
- Implement reconnection strategies

### Part 6: Message Acknowledgment (ACKs)
- Client acknowledgment modes (auto vs manual)
- Ensuring message delivery
- Retry mechanisms for failed deliveries
- Idempotency for duplicate messages

### Part 7: Scaling with External Message Broker
- Replace simple broker with RabbitMQ or Redis
- Handle multiple server instances
- Pub/sub across nodes
- Message persistence

### Part 8: Testing WebSockets
- Unit testing controllers with `@WebMvcTest`
- Integration testing with `WebSocketStompClient`
- Testing authentication flow
- Mocking message templates

### Part 9: Production Considerations
- Rate limiting to prevent abuse
- Message size limits
- Monitoring & metrics (Micrometer/Prometheus)
- Logging best practices
- SSL/TLS configuration
- Load balancing with sticky sessions

---

## Project Structure

```
electricity-price-monitor/
├── src/main/java/com/trading/priceMonitor/
│   ├── config/
│   │   ├── WebSocketConfig.java
│   │   ├── WebSocketEventListener.java
│   │   └── SecurityConfig.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   └── OrderController.java
│   ├── model/
│   │   ├── ElectricityPrice.java
│   │   ├── Order.java
│   │   ├── OrderConfirmation.java
│   │   └── Status.java
│   ├── security/
│   │   ├── JwtService.java
│   │   ├── JwtHandshakeInterceptor.java
│   │   └── UserInterceptor.java
│   └── service/
│       └── PriceService.java
├── src/main/resources/
│   └── static/
│       └── index.html
└── docs/
    ├── AUTH_FLOW.md
    └── LEARNING_ROADMAP.md
```

---

## Key Concepts Learned

| Concept | Description |
|---------|-------------|
| **STOMP** | Simple Text Oriented Messaging Protocol - adds structure to WebSocket |
| **Message Broker** | Routes messages between clients and server |
| **Topics** | Broadcast to all subscribers (`/topic/prices`) |
| **Queues** | Private messages to specific users (`/queue/order-confirmation`) |
| **Principal** | Represents the authenticated user |
| **Handshake Interceptor** | Runs during WebSocket upgrade |
| **Channel Interceptor** | Runs on every STOMP message |
