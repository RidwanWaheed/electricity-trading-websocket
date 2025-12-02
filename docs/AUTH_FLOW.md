# WebSocket JWT Authentication Flow

This document explains how all the authentication classes work together.

## Complete Auth Flow

### Step 1: Login (REST API)

```
Client                         AuthController                    JwtService
  │                                 │                                │
  │  POST /api/auth/login           │                                │
  │  {"username":"trader1",         │                                │
  │   "password":"password1"}       │                                │
  │ ───────────────────────────────>│                                │
  │                                 │                                │
  │                                 │  generateToken("trader1")      │
  │                                 │ ──────────────────────────────>│
  │                                 │                                │
  │                                 │  eyJhbGciOiJIUzI1NiJ9...       │
  │                                 │ <──────────────────────────────│
  │                                 │                                │
  │  {"token":"eyJ...", "username":"trader1"}                        │
  │ <───────────────────────────────│                                │
```

### Step 2: WebSocket Handshake

```
Client                    JwtHandshakeInterceptor              JwtService
  │                                 │                                │
  │  GET /ws-electricity?token=eyJ..│                                │
  │  (HTTP Upgrade to WebSocket)    │                                │
  │ ───────────────────────────────>│                                │
  │                                 │                                │
  │                                 │  isTokenValid("eyJ...")        │
  │                                 │ ──────────────────────────────>│
  │                                 │                                │
  │                                 │  true                          │
  │                                 │ <──────────────────────────────│
  │                                 │                                │
  │                                 │  extractUsername("eyJ...")     │
  │                                 │ ──────────────────────────────>│
  │                                 │                                │
  │                                 │  "trader1"                     │
  │                                 │ <──────────────────────────────│
  │                                 │                                │
  │                                 │  sessionAttributes.put(        │
  │                                 │    "username", "trader1")      │
  │                                 │                                │
  │  WebSocket Connection OK        │                                │
  │ <───────────────────────────────│                                │
```

### Step 3: STOMP Connect

```
Client                       UserInterceptor
  │                                 │
  │  STOMP CONNECT frame            │
  │ ───────────────────────────────>│
  │                                 │
  │                                 │  Read sessionAttributes
  │                                 │  username = "trader1"
  │                                 │
  │                                 │  Create StompPrincipal
  │                                 │  accessor.setUser(
  │                                 │    new StompPrincipal("trader1"))
  │                                 │
  │  STOMP CONNECTED                │
  │ <───────────────────────────────│
```

### Step 4: Sending Messages

```
Client                       OrderController
  │                                 │
  │  STOMP SEND /app/order          │
  │  {"orderId":"123",...}          │
  │ ───────────────────────────────>│
  │                                 │
  │                                 │  handleOrder(order, principal)
  │                                 │  principal.getName() → "trader1"
  │                                 │
  │                                 │  convertAndSendToUser(
  │                                 │    "trader1",
  │                                 │    "/queue/order-confirmation",
  │                                 │    confirmation)
  │                                 │
  │  MESSAGE /user/queue/order-confirmation
  │  (Only trader1 receives this!)  │
  │ <───────────────────────────────│
```

## Where Does SecurityConfig Fit?

SecurityConfig acts as the GATEKEEPER at the HTTP level:

```
┌─────────────────────────────────────────────────────────┐
│                    SecurityConfig                       │
│                                                         │
│  /api/auth/** ──────> PERMIT (no auth needed)           │
│  /ws/**       ──────> PERMIT (auth via JWT in query)    │
│  /ws-electricity/** ─> PERMIT (auth via JWT in query)   │
│  everything else ───> REQUIRES AUTHENTICATION           │
│                                                         │
│  + CSRF disabled (not needed for JWT)                   │
│  + Stateless sessions (JWT is self-contained)           │
└─────────────────────────────────────────────────────────┘
```

## Summary: Each Class's Role

| Class | Role |
|-------|------|
| **JwtService** | Creates & validates JWT tokens (the "passport factory") |
| **AuthController** | Login endpoint - exchanges credentials for JWT |
| **SecurityConfig** | HTTP gatekeeper - what URLs need auth |
| **JwtHandshakeInterceptor** | Validates JWT during WebSocket upgrade, stores username |
| **UserInterceptor** | Converts stored username into `Principal` for Spring |
| **WebSocketConfig** | Wires the interceptors together |

## Key Insight

**HTTP auth and WebSocket auth are separate.**

- `SecurityConfig` handles HTTP-level security
- `JwtHandshakeInterceptor` + `UserInterceptor` handle WebSocket authentication

The JWT token bridges both worlds: obtained via HTTP login, validated during WebSocket handshake.
