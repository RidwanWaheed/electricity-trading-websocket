# Phase 3: Microservices Architecture (Simplified)

## Overview

Evolve the monolithic application into a microservices architecture with 3 core services. Focus on the patterns that matter for interviews: service decomposition, async messaging, and distributed request tracking.

## Architecture

```
                                    ┌─────────────────┐
                                    │   Frontend      │
                                    │   (Browser)     │
                                    └────────┬────────┘
                                             │ STOMP/WebSocket
                                             ▼
┌────────────────────────────────────────────────────────────────────────────────┐
│                              Docker Network                                     │
│                                                                                │
│  ┌─────────────────────────────────────────────────────────────────┐          │
│  │                      Gateway Service                             │          │
│  │  • WebSocket/STOMP endpoint                                      │          │
│  │  • REST API (/api/auth/*, /api/orders/*)                        │          │
│  │  • JWT auth (generate + validate)                                │          │
│  │  • Price generation (scheduled)                                  │          │
│  │  • Routes orders to Order Service via RabbitMQ                   │          │
│  │  Port: 8080                                                      │          │
│  └──────────────────────────┬──────────────────────────────────────┘          │
│                             │ AMQP                                             │
│                             ▼                                                  │
│              ┌──────────────────────────────────────────┐                     │
│              │              RabbitMQ                    │                     │
│              │  • orders.topic                          │                     │
│              │  • m7.topic                              │                     │
│              └──────────────────────────────────────────┘                     │
│                      ▲                        ▲                               │
│                      │ AMQP                   │ AMQP                          │
│          ┌───────────┴───────┐    ┌───────────┴───────┐                       │
│          │  Order Service    │    │  Mock M7 Service  │                       │
│          │  • Persist orders │    │  • Simulates EPEX │                       │
│          │  • Business logic │    │  • Ack + Execute  │                       │
│          │  Port: 8081       │    │  Port: 8082       │                       │
│          └─────────┬─────────┘    └───────────────────┘                       │
│                    │ JPA                                                       │
│                    ▼                                                           │
│          ┌─────────────────┐                                                   │
│          │   PostgreSQL    │                                                   │
│          └─────────────────┘                                                   │
└────────────────────────────────────────────────────────────────────────────────┘
```

---

## Services Overview

| Service | Port | Responsibility | Database |
|---------|------|---------------|----------|
| **Gateway** | 8080 | WebSocket, REST, Auth, Prices | Users table |
| **Order Service** | 8081 | Order processing, persistence | Orders table |
| **Mock M7** | 8082 | Simulate trading engine | None |

---

## Task List

### Part 1: Project Restructuring

- [ ] Create multi-module Maven project structure:
  ```
  electricity-trading/
  ├── pom.xml (parent)
  ├── gateway-service/
  ├── order-service/
  ├── mock-m7-service/
  └── common/  (shared DTOs, constants)
  ```
- [ ] Create parent `pom.xml` with shared dependencies
- [ ] Create `common` module:
  - [ ] Shared DTOs (OrderMessage, OrderStatus, PriceUpdate)
  - [ ] RabbitMQ constants (exchange names, routing keys)
- [ ] Move existing monolith code to `gateway-service` as starting point
- [ ] Verify build works: `./mvnw clean package`

---

### Part 2: Gateway Service

The Gateway is the entry point. It keeps auth, WebSocket, and price generation.

- [ ] Configure as standalone Spring Boot app
- [ ] Keep from monolith:
  - [ ] WebSocket/STOMP configuration
  - [ ] JWT generation and validation
  - [ ] User entity, repository, AuthService
  - [ ] Price generation (scheduled task)
  - [ ] REST endpoints: `/api/auth/*`
- [ ] Add RabbitMQ integration:
  - [ ] Publish orders to `orders.topic` with key `order.submit`
  - [ ] Listen for `order.status.{userId}` → push to WebSocket
- [ ] Remove from monolith:
  - [ ] Order entity, repository, OrderService (moves to Order Service)
  - [ ] OrderConsumer (moves to Order Service)
- [ ] Create Dockerfile
- [ ] Health check endpoint: `/actuator/health`

---

### Part 3: Order Service

Handles order persistence and communicates with Mock M7.

- [ ] Create `order-service` module
- [ ] Move from monolith:
  - [ ] Order entity and repository
  - [ ] Order validation logic
- [ ] RabbitMQ listeners:
  - [ ] `order.submit` - receive orders from Gateway
  - [ ] `m7.response.ack` - M7 acknowledged order
  - [ ] `m7.response.fill` - M7 filled/rejected order
- [ ] RabbitMQ publishers:
  - [ ] `m7.request.order` - send to Mock M7
  - [ ] `order.status.{userId}` - notify Gateway of status changes
- [ ] Order state machine:
  ```
  PENDING → SUBMITTED → FILLED
                     → REJECTED
  ```
- [ ] Add correlation ID to track orders across services
- [ ] Create Dockerfile
- [ ] Health check endpoint

---

### Part 4: Mock M7 Service

Simulates the EPEX SPOT trading engine behavior.

- [ ] Create `mock-m7-service` module
- [ ] RabbitMQ listener:
  - [ ] `m7.request.order` - receive order requests
- [ ] RabbitMQ publishers:
  - [ ] `m7.response.ack` - immediate acknowledgment
  - [ ] `m7.response.fill` - delayed fill/reject (after random delay)
- [ ] Simulate realistic behavior:
  - [ ] Immediate ACK (< 50ms)
  - [ ] Random fill delay (500ms - 2s)
  - [ ] 90% fill rate, 10% reject rate
- [ ] Preserve correlation ID from request
- [ ] Create Dockerfile
- [ ] Health check endpoint

---

### Part 5: RabbitMQ Configuration

- [ ] Define exchanges:
  ```
  orders.topic  - for order flow (Gateway ↔ Order Service)
  m7.topic      - for M7 communication (Order Service ↔ Mock M7)
  ```
- [ ] Define queues:
  ```
  order.submissions      - Order Service listens
  order.m7-responses     - Order Service listens for M7 responses
  m7.requests            - Mock M7 listens
  gateway.order-status   - Gateway listens for status updates
  ```
- [ ] Define routing keys:
  ```
  order.submit           - new order from Gateway
  order.status.{userId}  - status update for specific user
  m7.request.order       - order to M7
  m7.response.ack        - M7 acknowledgment
  m7.response.fill       - M7 fill/reject
  ```

---

### Part 6: Docker Compose

- [ ] Update `docker-compose.yml`:
  ```yaml
  services:
    gateway:
      build: ./gateway-service
      ports: ["8080:8080"]
      depends_on: [rabbitmq, postgres]
      environment:
        SPRING_PROFILES_ACTIVE: docker

    order-service:
      build: ./order-service
      ports: ["8081:8081"]
      depends_on: [rabbitmq, postgres]

    mock-m7:
      build: ./mock-m7-service
      ports: ["8082:8082"]
      depends_on: [rabbitmq]

    rabbitmq:
      image: rabbitmq:3-management
      ports: ["5672:5672", "15672:15672"]

    postgres:
      image: postgres:16
      environment:
        POSTGRES_DB: trading
        POSTGRES_USER: trading
        POSTGRES_PASSWORD: trading
  ```
- [ ] Add health checks for all services
- [ ] Verify full stack starts: `docker-compose up -d`

---

### Part 7: Correlation IDs

Track requests across all services for debugging and observability.

- [ ] Generate correlation ID in Gateway when order received
- [ ] Pass correlation ID in RabbitMQ message headers
- [ ] Log correlation ID in all services
- [ ] Include correlation ID in order status responses
- [ ] Example log output:
  ```
  [gateway]  [corr-id=abc123] Order received from user trader1
  [order]    [corr-id=abc123] Order saved with status PENDING
  [order]    [corr-id=abc123] Published to M7
  [mock-m7]  [corr-id=abc123] Order acknowledged
  [mock-m7]  [corr-id=abc123] Order filled after 1.2s
  [order]    [corr-id=abc123] Status updated to FILLED
  [gateway]  [corr-id=abc123] Pushed status to user via WebSocket
  ```

---

### Part 8: Testing & Verification

- [ ] Manual end-to-end test:
  1. Start all services with docker-compose
  2. Login via REST, get JWT
  3. Connect WebSocket with JWT
  4. Submit order
  5. Verify status updates arrive via WebSocket
- [ ] Verify in RabbitMQ Management UI (localhost:15672):
  - [ ] Exchanges created
  - [ ] Queues bound correctly
  - [ ] Messages flowing through
- [ ] Check logs for correlation ID flow

---

### Part 9: Documentation

- [ ] Update main README with new architecture diagram
- [ ] Document how to run the microservices stack
- [ ] Document the message flow

---

## Message Flow

### Order Submission:
```
1. Browser → Gateway: Submit order via WebSocket /app/order
2. Gateway: Generate correlation ID, publish to RabbitMQ
3. Order Service: Consume, validate, save as PENDING
4. Order Service → Mock M7: Publish order request
5. Mock M7: ACK immediately
6. Order Service: Update to SUBMITTED, notify Gateway
7. Gateway → Browser: Push status via WebSocket
8. Mock M7: Fill after delay
9. Order Service: Update to FILLED, notify Gateway
10. Gateway → Browser: Push final status
```

### Price Broadcast:
```
1. Gateway: Generate price (scheduled)
2. Gateway → All Browsers: Broadcast via /topic/prices
```

---

## Interview Talking Points

After completing Phase 3, you can confidently discuss:

1. **Service Decomposition** - "I broke a monolith into focused services based on bounded contexts"
2. **API Gateway Pattern** - "Single entry point handles auth, WebSocket, and routes to internal services"
3. **Async Messaging** - "Services communicate via RabbitMQ for loose coupling"
4. **Correlation IDs** - "I track requests across services using correlation IDs in message headers"
5. **Event-Driven Architecture** - "Order status changes propagate through events, not direct calls"
6. **Docker Orchestration** - "All services run in Docker with proper dependency ordering"

---

## Commands Reference

```bash
# Build all modules
./mvnw clean package -DskipTests

# Start full stack
docker-compose up -d

# View logs
docker-compose logs -f gateway
docker-compose logs -f order-service
docker-compose logs -f mock-m7

# Rebuild single service
docker-compose up -d --build gateway

# Stop all
docker-compose down

# RabbitMQ Management
open http://localhost:15672  # guest/guest
```

---

## Stretch Goals (Optional)

- [ ] Add dead letter queue for failed messages
- [ ] Add retry logic with exponential backoff
- [ ] Add basic distributed tracing (log correlation)
- [ ] Scale Order Service to 2 instances
