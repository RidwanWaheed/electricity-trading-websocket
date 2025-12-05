# Architecture Decision Records

Key technical decisions made during development.

---

## ADR-001: Java Records for DTOs

**Decision:** Use Java records instead of classes/Lombok for data transfer objects.

**Rationale:**
- Immutable by default - safer for message passing
- Built into Java 21 - no external dependency
- Compact constructor for validation
- Less boilerplate than Lombok `@Value`

---

## ADR-002: Explicit Getters for JPA Entities (No Lombok)

**Decision:** Write explicit getters for `OrderEntity` instead of using Lombok.

**Rationale:**
- Entity has state machine semantics (`PENDING → SUBMITTED → FILLED`)
- Methods like `markSubmitted()`, `markFilled()` enforce valid transitions
- Exposing setters (even via Lombok) would bypass business logic
- Lombok + JPA can cause issues with `equals()`/`hashCode()` and lazy loading

---

## ADR-003: No Database for Mock M7

**Decision:** Mock M7 Service is stateless - no database persistence.

**Rationale:**
- Simulates external trading engine (EPEX SPOT) which we don't control
- Only needs to receive orders and send ACK/FILL responses
- Order Service is the source of truth for order state
- Reduces complexity without sacrificing learning value

---

## ADR-004: Gateway Keeps Auth and Prices

**Decision:** Auth and price generation stay in Gateway instead of separate services.

**Rationale:**
- Extracting them adds complexity without clear benefit at this scale
- Auth is tightly coupled with WebSocket handshake (JWT validation)
- Price generation is simple scheduled task, not worth a separate service
- Focus learning on the patterns that matter: async messaging, correlation IDs

---

## ADR-005: Topic Exchange with Routing Keys

**Decision:** Use RabbitMQ topic exchanges instead of direct exchanges.

**Rationale:**
- Flexible routing: `order.status.*` receives all user status updates
- Pattern matching: `m7.response.*` receives both ACK and FILL
- Easier to add new message types without reconfiguring exchanges
- Mirrors real-world event-driven architectures

---

## ADR-006: Correlation ID for Distributed Tracing

**Decision:** Generate correlation ID at Gateway, propagate through all services.

**Rationale:**
- Essential for debugging distributed systems
- Appears in all logs: `[corr-id=abc123] Order received...`
- Passed via message payload (not headers) for simplicity
- Foundation for future observability (could integrate with Sleuth/Zipkin)

---

## ADR-007: Two-Phase M7 Response (ACK + FILL)

**Decision:** Mock M7 sends immediate ACK, then delayed FILL/REJECT.

**Rationale:**
- Mirrors real trading engine behavior
- Teaches eventual consistency - ACK ≠ execution
- Order Service must handle both messages and update state accordingly
- More realistic than single synchronous response

---

## ADR-008: BigDecimal for Financial Data

**Decision:** Use `BigDecimal` for price and quantity, never `double`/`float`.

**Rationale:**
- Floating point arithmetic causes rounding errors
- Financial calculations require exact precision
- Industry standard for money/trading applications
- `0.1 + 0.2 != 0.3` with doubles, but works correctly with BigDecimal

---

## ADR-009: Multi-Module Maven Structure

**Decision:** Single repository with Maven modules instead of separate repos.

**Rationale:**
- Easier to refactor and share code (common module)
- Single `./mvnw package` builds everything
- Atomic commits across services during development
- Can split into separate repos later if needed

---

## ADR-010: User-Specific Routing via Username in Routing Key

**Decision:** Route status updates using `order.status.{username}` pattern.

**Rationale:**
- Gateway receives all status updates on one queue
- Routing key contains username for logging/debugging
- Gateway extracts username from message to route to correct WebSocket session
- Alternative (queue per user) doesn't scale well
