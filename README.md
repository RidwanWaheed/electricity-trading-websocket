# Electricity Trading WebSocket

A practice project for implementing real-time WebSocket communication, built as a foundation for a larger electricity trading platform that connects to the **M7 EPEX SPOT market**.

## Background

Our main project is an electricity trading platform currently in **alpha phase**. Initially, we implemented polling to stream live market data to the frontend with plans to "defer WebSocket for later." We quickly realized this approach would accumulate significant technical debt—polling doesn't scale well for real-time trading where milliseconds matter.

This repository serves as a hands-on learning project to understand WebSocket/STOMP patterns before integrating them into the production trading platform.

## Features

- **Real-time Price Broadcasting** - Server pushes electricity prices to all connected clients via `/topic/prices`
- **WebSocket Messaging Demo** - Demonstrates client-to-server messaging patterns (`/app/order`)
- **User-specific Messages** - Private messages sent only to the originating user (`/user/queue/order-confirmation`)
- **JWT Authentication** - Secure WebSocket connections with token-based auth
- **Client-side Order Tracking** - Correlates confirmations with original orders by orderId (no data duplication over the wire)
- **Auto-reconnection** - Exponential backoff reconnection (1s, 2s, 4s...) with max 5 attempts
- **Error Handling** - Graceful error propagation to clients (`/user/queue/errors`)
- **Heartbeat** - Connection health monitoring with 10-second intervals

> **Note:** This demo sends orders via WebSocket for learning purposes. In production, orders should be submitted via REST for reliability, while WebSocket handles real-time updates (prices, trade confirmations, order status changes).

## Tech Stack

- Java 21
- Spring Boot 3.5
- Spring WebSocket with STOMP protocol
- Spring Security
- JWT (jjwt library)

## Running the Project

```bash
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`

## API Endpoints

### REST
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Authenticate and receive JWT |

### WebSocket (STOMP)
| Destination | Direction | Description |
|-------------|-----------|-------------|
| `/topic/prices` | Server → Client | Subscribe for price updates |
| `/app/order` | Client → Server | Demo: send messages to server |
| `/user/queue/order-confirmation` | Server → Client | Receive private confirmations |
| `/user/queue/errors` | Server → Client | Receive error messages |

### Production Architecture (Recommended)
```
┌────────┐         ┌─────────────┐         ┌──────────┐
│ Client │         │   Backend   │         │ M7 EPEX  │
└───┬────┘         └──────┬──────┘         └────┬─────┘
    │                     │                     │
    │ ── REST ──────────> │ ── API ──────────>  │  Orders
    │                     │                     │
    │ <── WebSocket ───── │ <─────────────────  │  Updates
    │   (prices, trades)  │                     │
```

## Learning Roadmap

See [docs/LEARNING_ROADMAP.md](docs/LEARNING_ROADMAP.md) for the complete learning path.

### Completed
- [x] Server → Client broadcasting (STOMP topics)
- [x] Client → Server messaging (@MessageMapping)
- [x] User-specific messages (queues)
- [x] Error handling
- [x] JWT Authentication
- [x] Heartbeat & Connection Management
- [x] Client-side Order Tracking (orderId correlation)
- [x] Auto-reconnection with exponential backoff

## Why WebSocket over Polling?

| Aspect | Polling | WebSocket |
|--------|---------|-----------|
| Latency | High (interval-based) | Low (instant push) |
| Server Load | High (constant requests) | Low (persistent connection) |
| Bandwidth | Wasteful (repeated headers) | Efficient (minimal overhead) |
| Real-time Trading | Not suitable | Ideal |

For electricity trading where prices change rapidly and order execution speed matters, WebSocket is the clear choice.

## Related Documentation

- [Authentication Flow](docs/AUTH_FLOW.md) - How JWT auth works with WebSocket
- [Learning Roadmap](docs/LEARNING_ROADMAP.md) - Topics covered and upcoming
