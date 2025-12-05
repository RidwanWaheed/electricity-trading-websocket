# Electricity Trading WebSocket

A practice project for implementing real-time WebSocket communication, built as a foundation for a larger electricity trading platform that connects to the **M7 EPEX SPOT market**.

![Frontend Screenshot](assets/screenshot.png)

## Background

Our main project is an electricity trading platform currently in **alpha phase**. Initially, we implemented polling to stream live market data to the frontend with plans to "defer WebSocket for later." We quickly realized this approach would accumulate significant technical debt—polling doesn't scale well for real-time trading where milliseconds matter.

This repository serves as a hands-on learning project to understand WebSocket/STOMP patterns before integrating them into the production trading platform.

## Tech Stack

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-FF6600?style=flat&logo=rabbitmq&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat&logo=docker&logoColor=white)

## Architecture

```
┌─────────────┐       WebSocket/STOMP        ┌─────────────────────────────────────┐
│   Browser   │◄────────────────────────────►│           Spring Boot               │
│             │    • /topic/prices           │                                     │
│  - SockJS   │    • /user/queue/orders      │  ┌──────────┐    ┌───────────────┐  │
│  - STOMP    │    • /app/order              │  │ WebSocket│    │ Order Service │  │
└─────────────┘                              │  │ Handler  │───►│               │  │
                                             │  └──────────┘    └──────┬────────┘  │
      │                                      │                         │           │
      │ REST API                             │  ┌─────────┐            │           │
      │ • /api/auth/*                        │  │  Price  │            │           │
      └─────────────────────────────────────►│  │ Service │            │           │
                                             │  └────┬────┘            │           │
                                             └───────┼─────────────────│───────────┘
                                                     │                 │
                                         publish     │                 │ consume/publish
                                                     ▼                 ▼
                                             ┌─────────────────────────────┐
                                             │         RabbitMQ            │
                                             │  • trading.prices (fanout)  │
                                             │  • trading.orders (direct)  │
                                             └─────────────────────────────┘
                                                            │
                                                            │ persist
                                                            ▼
                                             ┌─────────────────────────────┐
                                             │        PostgreSQL           │
                                             │  • users, orders tables     │
                                             └─────────────────────────────┘
```

## Features

- **Real-time Price Broadcasting** - Server pushes electricity prices to all connected clients
- **WebSocket + STOMP** - Bi-directional messaging with topic subscriptions
- **User-specific Messages** - Private order confirmations per user session
- **JWT Authentication** - Secure WebSocket connections with token-based auth
- **Auto-reconnection** - Exponential backoff (1s, 2s, 4s...) with max 5 attempts
- **Message Queuing** - RabbitMQ for order processing pipeline

## Quick Start

```bash
make up      # Start full Docker stack
```

The application starts on `http://localhost:8080`

### Local Development
```bash
make deps    # Start PostgreSQL + RabbitMQ
make run     # Run the app
```

## Make Commands

| Command | Description |
|---------|-------------|
| `make up` | Start full Docker stack |
| `make down` | Stop all containers |
| `make deps` | Start only postgres + rabbitmq |
| `make run` | Run app locally |
| `make test` | Run unit tests |
| `make fmt` | Auto-format code |
| `make build` | Build JAR |
| `make logs` | Tail app container logs |

## API Endpoints

### REST
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Authenticate and receive JWT |
| POST | `/api/auth/register` | Register new user |
| GET | `/api/orders` | Get user's order history |
| GET | `/api/orders/{orderId}` | Get specific order |
| DELETE | `/api/orders/{orderId}` | Cancel an order |

### WebSocket (STOMP)
| Destination | Direction | Description |
|-------------|-----------|-------------|
| `/topic/prices` | Server → Client | Subscribe for price updates |
| `/app/order` | Client → Server | Submit order |
| `/user/queue/order-confirmation` | Server → Client | Receive order confirmations |
| `/user/queue/errors` | Server → Client | Receive error messages |

## CI/CD

GitHub Actions runs on push/PR:
1. Code formatting (Spotless)
2. Unit tests
3. Build
4. Docker image (main branch)
