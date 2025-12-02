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

### Part 5: Heartbeat & Connection Management
- [x] STOMP heartbeats (10s server, 10s client)
- [x] Dedicated heartbeat thread pool
- [x] Client-side auto-reconnection with exponential backoff
- [x] Connection state UI feedback (connected/reconnecting/disconnected)

### Part 6: Testing
- [x] Unit tests for `JwtService`
- [x] WebSocket integration tests with `WebSocketStompClient`
- [x] Authentication flow testing

---

## Upcoming Topics

### Part 7: Message Acknowledgment (ACKs)
- Client acknowledgment modes (auto vs manual)
- Ensuring message delivery
- Retry mechanisms for failed deliveries
- Idempotency for duplicate messages

### Part 8: Scaling with External Message Broker
- Replace simple broker with RabbitMQ or Redis
- Handle multiple server instances
- Pub/sub across nodes
- Message persistence

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
electricity-trading-websocket/
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
│   │   ├── UserInterceptor.java
│   │   └── StompPrincipal.java
│   └── service/
│       └── PriceService.java
├── src/main/resources/
│   └── static/
│       ├── index.html
│       ├── css/
│       │   └── style.css
│       └── js/
│           └── app.js
├── src/test/java/
│   └── com/trading/priceMonitor/
│       ├── security/
│       │   └── JwtServiceTest.java
│       └── WebSocketIntegrationTest.java
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
| **Exponential Backoff** | Reconnection strategy: 1s, 2s, 4s, 8s, 16s delays |
