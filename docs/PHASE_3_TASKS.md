# Phase 3: Microservices Architecture

## Overview

Evolve the monolithic application into a microservices architecture, simulating a real trading system like M7. This phase teaches service decomposition, inter-service communication via RabbitMQ, and distributed system patterns.

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
│  ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐        │
│  │  Gateway Service │      │  Auth Service   │      │  Price Service  │        │
│  │  (WebSocket +    │      │  (JWT + Users)  │      │  (Generation)   │        │
│  │   API Gateway)   │      │  Port: 8081     │      │  Port: 8082     │        │
│  │  Port: 8080      │      └────────┬────────┘      └────────┬────────┘        │
│  └────────┬─────────┘               │                        │                 │
│           │                         │ AMQP                   │ AMQP            │
│           │ AMQP                    ▼                        ▼                 │
│           │              ┌──────────────────────────────────────────┐          │
│           └─────────────►│              RabbitMQ                    │          │
│                          │  Exchanges: orders, prices, auth        │          │
│                          └──────────────────────────────────────────┘          │
│                                      ▲                        ▲                │
│                                      │ AMQP                   │ AMQP           │
│                          ┌───────────┴───────┐    ┌───────────┴───────┐        │
│                          │  Order Service    │    │  Mock M7 Service  │        │
│                          │  (Processing)     │    │  (Trading Engine) │        │
│                          │  Port: 8083       │    │  Port: 8084       │        │
│                          └─────────┬─────────┘    └───────────────────┘        │
│                                    │                                           │
│                                    │ JPA                                       │
│                                    ▼                                           │
│                          ┌─────────────────┐                                   │
│                          │   PostgreSQL    │                                   │
│                          └─────────────────┘                                   │
└────────────────────────────────────────────────────────────────────────────────┘
```

---

## Services Overview

| Service | Responsibility | Database | Listens To | Publishes To |
|---------|---------------|----------|------------|--------------|
| **Gateway** | WebSocket connections, route API requests | None | `order.status.*`, `price.*` | `order.submit` |
| **Auth** | JWT generation, user management | Users table | `auth.validate` | `auth.response` |
| **Price** | Generate mock electricity prices | None | - | `price.{region}` |
| **Order** | Validate & persist orders | Orders table | `order.submit`, `m7.response.*` | `m7.request.order` |
| **Mock M7** | Simulate trading engine responses | None | `m7.request.*` | `m7.response.*` |

---

## Task List

### Part 1: Project Restructuring

- [ ] Create multi-module Maven project structure:
  ```
  electricity-trading/
  ├── pom.xml (parent)
  ├── gateway-service/
  ├── auth-service/
  ├── price-service/
  ├── order-service/
  ├── mock-m7-service/
  └── common/  (shared DTOs, utils)
  ```
- [ ] Create parent `pom.xml` with shared dependencies
- [ ] Create `common` module for shared code:
  - [ ] DTOs (OrderRequest, PriceUpdate, etc.)
  - [ ] Constants (queue names, exchange names)
  - [ ] RabbitMQ configuration base class
- [ ] Update `.gitignore` for multi-module structure

### Part 2: Gateway Service

- [ ] Create `gateway-service` module
- [ ] Move WebSocket configuration from monolith
- [ ] Move JWT validation (validate only, not generate)
- [ ] Implement RabbitMQ listeners:
  - [ ] Listen to `price.*` → push to WebSocket `/topic/prices`
  - [ ] Listen to `order.status.{userId}` → push to WebSocket `/user/queue/orders`
- [ ] Implement RabbitMQ publishers:
  - [ ] Publish order submissions to `order.submit`
- [ ] Remove direct service dependencies (no PriceService, OrderService)
- [ ] Add health check endpoint
- [ ] Create Dockerfile

### Part 3: Auth Service

- [ ] Create `auth-service` module
- [ ] Move user entity and repository from monolith
- [ ] Move JWT generation logic
- [ ] Implement REST endpoints:
  - [ ] `POST /auth/login` - generate JWT
  - [ ] `POST /auth/register` - create user
  - [ ] `GET /auth/validate` - validate token (for other services)
- [ ] Implement RabbitMQ listener (optional):
  - [ ] Listen to `auth.validate` for async token validation
- [ ] Create Dockerfile

### Part 4: Price Service

- [ ] Create `price-service` module
- [ ] Move price generation logic from monolith
- [ ] Remove WebSocket dependency (no SimpMessagingTemplate)
- [ ] Publish prices to RabbitMQ:
  - [ ] Exchange: `prices.topic`
  - [ ] Routing keys: `price.NORTH`, `price.SOUTH`, `price.EAST`, `price.WEST`
- [ ] Configurable price generation interval
- [ ] Create Dockerfile

### Part 5: Order Service

- [ ] Create `order-service` module
- [ ] Move order entity and repository from monolith
- [ ] Implement RabbitMQ listeners:
  - [ ] Listen to `order.submit` from Gateway
  - [ ] Listen to `m7.response.ack` for order acknowledgments
  - [ ] Listen to `m7.response.execution` for order status updates
- [ ] Implement RabbitMQ publishers:
  - [ ] Publish to `m7.request.order` (submit to M7)
  - [ ] Publish to `order.status.{userId}` (notify user via Gateway)
- [ ] Order workflow:
  ```
  1. Receive order from Gateway
  2. Validate order
  3. Save with status PENDING
  4. Publish to Mock M7
  5. Receive M7 acknowledgment → update status to SUBMITTED
  6. Receive M7 execution → update status to FILLED/REJECTED
  7. Notify user via Gateway
  ```
- [ ] Create Dockerfile

### Part 6: Mock M7 Service

- [ ] Create `mock-m7-service` module
- [ ] Implement order book simulation:
  - [ ] Simple in-memory order matching
  - [ ] Random fill delays (simulate market)
  - [ ] Partial fills support (optional)
- [ ] Implement RabbitMQ listeners:
  - [ ] Listen to `m7.request.order` - receive new orders
- [ ] Implement RabbitMQ publishers:
  - [ ] Publish to `m7.response.ack` - immediate acknowledgment
  - [ ] Publish to `m7.response.execution` - order filled/rejected (after delay)
- [ ] Simulate realistic M7 behavior:
  - [ ] 90% orders filled, 10% rejected
  - [ ] Random execution delay (100ms - 2s)
  - [ ] Order execution reports (like OrdrExeRprt)
- [ ] Create Dockerfile

### Part 7: RabbitMQ Configuration

- [ ] Define exchanges:
  ```
  orders.topic    - for order flow
  prices.topic    - for price broadcasts
  m7.topic        - for M7 communication
  ```
- [ ] Define queues per service:
  ```
  gateway.prices          - Gateway listens for prices
  gateway.order-status    - Gateway listens for order updates
  order.submissions       - Order Service listens for new orders
  order.m7-responses      - Order Service listens for M7 responses
  m7.requests             - Mock M7 listens for order requests
  ```
- [ ] Define routing keys:
  ```
  price.{region}          - e.g., price.NORTH
  order.submit            - new order submission
  order.status.{userId}   - order status for specific user
  m7.request.order        - order to M7
  m7.response.ack         - M7 acknowledgment
  m7.response.execution   - M7 order execution
  ```
- [ ] Create `docs/RABBITMQ_TOPOLOGY.md` with diagrams

### Part 8: Docker Compose

- [ ] Update `docker-compose.yml`:
  ```yaml
  services:
    gateway:
      build: ./gateway-service
      ports: ["8080:8080"]
      depends_on: [rabbitmq, auth]

    auth:
      build: ./auth-service
      ports: ["8081:8081"]
      depends_on: [postgres]

    price:
      build: ./price-service
      depends_on: [rabbitmq]

    order:
      build: ./order-service
      depends_on: [rabbitmq, postgres]

    mock-m7:
      build: ./mock-m7-service
      depends_on: [rabbitmq]

    rabbitmq:
      image: rabbitmq:3-management
      ports: ["5672:5672", "15672:15672"]

    postgres:
      image: postgres:15
      environment:
        POSTGRES_DB: trading_db
  ```
- [ ] Add health checks for all services
- [ ] Configure service discovery (environment variables)
- [ ] Add docker-compose profiles (dev, test)

### Part 9: Service Communication Patterns

- [ ] Implement Request/Reply pattern (Order → M7 → Order):
  ```java
  // Synchronous-style with RabbitMQ
  rabbitTemplate.convertSendAndReceive(...)
  ```
- [ ] Implement Pub/Sub pattern (Price → all subscribers):
  ```java
  // Fire and forget broadcast
  rabbitTemplate.convertAndSend("prices.topic", "price.NORTH", priceUpdate)
  ```
- [ ] Implement correlation IDs for tracking:
  ```java
  // Track order through entire flow
  message.getMessageProperties().setCorrelationId(orderId)
  ```
- [ ] Add dead letter queues for failed messages
- [ ] Implement retry logic with exponential backoff

### Part 10: Testing

- [ ] Integration tests with Testcontainers:
  - [ ] Test complete order flow across services
  - [ ] Test price broadcast flow
- [ ] Contract tests between services
- [ ] Load testing with multiple concurrent orders
- [ ] Chaos testing (kill services, verify recovery)

### Part 11: Observability

- [ ] Add distributed tracing (Spring Cloud Sleuth / Micrometer)
- [ ] Centralized logging (all services log with correlation ID)
- [ ] Health endpoints for all services
- [ ] RabbitMQ monitoring via Management UI
- [ ] Create Grafana dashboard (optional)

### Part 12: Documentation

- [ ] Update README with microservices architecture
- [ ] Create `docs/SERVICE_COMMUNICATION.md`
- [ ] Create `docs/LOCAL_DEVELOPMENT.md`
- [ ] Document message schemas in `docs/MESSAGE_SCHEMAS.md`
- [ ] Add architecture decision records (ADRs)

---

## Key Concepts to Learn

| Concept | Description |
|---------|-------------|
| **Service Decomposition** | Breaking monolith into focused services |
| **API Gateway** | Single entry point for clients |
| **Event-Driven Architecture** | Services communicate via events/messages |
| **Saga Pattern** | Managing distributed transactions (order flow) |
| **Correlation ID** | Tracking requests across services |
| **Dead Letter Queue** | Handling failed messages |
| **Circuit Breaker** | Handling service failures gracefully |
| **Service Discovery** | Services finding each other |
| **Eventual Consistency** | Data consistency across services |

---

## Message Flow Examples

### Order Submission Flow:
```
1. Browser → Gateway: POST /api/orders (WebSocket or REST)
2. Gateway → RabbitMQ: publish to "order.submit"
3. Order Service ← RabbitMQ: consume from "order.submissions"
4. Order Service: validate, save as PENDING
5. Order Service → RabbitMQ: publish to "m7.request.order"
6. Mock M7 ← RabbitMQ: consume order request
7. Mock M7 → RabbitMQ: publish ack to "m7.response.ack"
8. Order Service ← RabbitMQ: consume ack, update to SUBMITTED
9. Order Service → RabbitMQ: publish to "order.status.{userId}"
10. Gateway ← RabbitMQ: consume status update
11. Gateway → Browser: push via WebSocket /user/queue/orders
```

### Price Broadcast Flow:
```
1. Price Service: generate new price
2. Price Service → RabbitMQ: publish to "price.NORTH"
3. Gateway ← RabbitMQ: consume from "gateway.prices"
4. Gateway → All Browsers: broadcast via WebSocket /topic/prices
```

---

## Commands Reference

```bash
# Build all services
./mvnw clean package -DskipTests

# Start all services
docker-compose up -d

# Start specific service
docker-compose up -d gateway order

# View logs for specific service
docker-compose logs -f order

# Scale a service
docker-compose up -d --scale price=2

# Stop all services
docker-compose down

# Rebuild single service
docker-compose up -d --build gateway

# Access RabbitMQ Management
open http://localhost:15672

# Check service health
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
```

---

## Prerequisites from Phase 2

Before starting Phase 3, ensure Phase 2 is complete:
- [x] Docker and docker-compose working
- [x] RabbitMQ running and understood
- [x] PostgreSQL with JPA entities
- [x] Basic AMQP publish/subscribe working
- [x] Testcontainers setup

---

## Stretch Goals

- [ ] Add Kubernetes manifests (Phase 4?)
- [ ] Implement API versioning
- [ ] Add rate limiting at Gateway
- [ ] Implement caching (Redis)
- [ ] Add WebSocket clustering (multiple Gateway instances)
- [ ] Implement CQRS for order queries
