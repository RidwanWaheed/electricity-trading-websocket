# Phase 2: RabbitMQ, Docker & Persistence

## Overview

Extend the WebSocket learning project with an external message broker, containerization, and database persistence.

## Architecture

```
┌──────────┐     STOMP/WS    ┌─────────────┐      AMQP       ┌──────────┐
│  Browser │ <──────────────>│  Spring Boot │<──────────────>│ RabbitMQ │
└──────────┘                 └──────┬──────┘                 └──────────┘
                                    │
                                    │ JPA
                                    ▼
                             ┌──────────────┐
                             │  PostgreSQL  │
                             │  - users     │
                             │  - orders    │
                             └──────────────┘
```

---

## Task List

### Part 1: Docker Setup
- [ ] Create `Dockerfile` for Spring Boot application
- [ ] Create `docker-compose.yml` with services:
  - [ ] Spring Boot app
  - [ ] RabbitMQ (with management plugin)
  - [ ] PostgreSQL
- [ ] Configure Docker networking between services
- [ ] Add `.dockerignore` file
- [ ] Update README with Docker instructions

### Part 2: RabbitMQ Integration
- [ ] Add Spring AMQP dependency to `pom.xml`
- [ ] Configure RabbitMQ connection properties
- [ ] Create RabbitMQ configuration class:
  - [ ] Define exchange (topic exchange for prices)
  - [ ] Define queues (price-updates, order-confirmations)
  - [ ] Define bindings (routing keys)
- [ ] Create `RabbitMQPublisher` service for publishing messages
- [ ] Create `RabbitMQListener` service for consuming messages
- [ ] Modify `PriceService` to publish prices to RabbitMQ
- [ ] Bridge RabbitMQ messages to WebSocket clients
- [ ] Test message flow: RabbitMQ → Spring → WebSocket → Browser

### Part 3: PostgreSQL & Persistence
- [ ] Add Spring Data JPA and PostgreSQL dependencies
- [ ] Configure datasource properties (with Docker profile)
- [ ] Create JPA entities:
  - [ ] `UserEntity` (id, username, passwordHash, createdAt)
  - [ ] `OrderEntity` (id, orderId, userId, region, type, quantity, price, status, createdAt)
- [ ] Create repositories:
  - [ ] `UserRepository`
  - [ ] `OrderRepository`
- [ ] Implement password hashing with BCrypt
- [ ] Migrate `AuthController` to use database users
- [ ] Persist orders in `OrderController`
- [ ] Add REST endpoint to fetch order history: `GET /api/orders`
- [ ] Update frontend to load order history on connect

### Part 4: Testing & Documentation
- [ ] Add Testcontainers for integration tests
- [ ] Write RabbitMQ integration tests
- [ ] Write repository tests
- [ ] Update `LEARNING_ROADMAP.md`
- [ ] Create `docs/RABBITMQ_FLOW.md` with message flow diagrams
- [ ] Update README with new architecture

---

## Key Concepts to Learn

| Concept | Description |
|---------|-------------|
| **AMQP** | Advanced Message Queuing Protocol - RabbitMQ's native protocol |
| **Exchange** | Routes messages to queues based on routing rules |
| **Queue** | Stores messages until consumed |
| **Binding** | Links exchange to queue with routing key |
| **Topic Exchange** | Routes based on pattern matching (e.g., `price.NORTH`) |
| **BCrypt** | Password hashing algorithm |
| **Testcontainers** | Disposable Docker containers for testing |

---

## Commands Reference

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down

# Rebuild after code changes
docker-compose up -d --build

# Access RabbitMQ Management UI
open http://localhost:15672  # guest/guest

# Access PostgreSQL
docker exec -it postgres psql -U trading -d trading_db
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `docker` | Active Spring profile |
| `RABBITMQ_HOST` | `rabbitmq` | RabbitMQ hostname |
| `POSTGRES_HOST` | `postgres` | PostgreSQL hostname |
| `POSTGRES_DB` | `trading_db` | Database name |
| `POSTGRES_USER` | `trading` | Database user |
| `POSTGRES_PASSWORD` | `trading123` | Database password |
