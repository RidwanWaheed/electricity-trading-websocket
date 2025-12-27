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

## ADR-004: Gateway Keeps Auth Only

**Decision:** Auth stays in Gateway; price generation moved to Mock M7.

**Rationale:**
- Auth is tightly coupled with WebSocket handshake (JWT validation)
- Prices should come from exchange (Mock M7), not be generated internally
- Gateway is a pass-through for market data, not a source
- Mirrors real trading systems where exchange is source of truth for prices

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

---

## ADR-011: Price Feed from Mock M7 (Market Data Pattern)

**Decision:** Prices originate from Mock M7 and flow through RabbitMQ to Gateway.

**Rationale:**
- Real trading systems receive prices from exchange, not generate them
- Separates market data (pub/sub broadcast) from order flow (request/response)
- Gateway becomes a pass-through, not a source of truth
- Teaches realistic market data distribution pattern
- Mock M7 publishes to `prices.topic`, Gateway subscribes and forwards to WebSocket

---

## ADR-012: JWT in localStorage (Development Trade-off)

**Decision:** Store JWT tokens in localStorage for this learning project.

**Rationale:**
- Simple to implement and debug (visible in DevTools)
- Works well for demonstrating token-based auth flow
- Acceptable for learning project with no real user data

**Trade-offs acknowledged:**
- Vulnerable to XSS (any JS on page can read token)
- No automatic CSRF protection

**Production alternative:**
- `httpOnly` cookies (JS can't access, browser sends automatically)
- Short-lived access tokens (15 min) + refresh tokens
- Token revocation on logout (requires Redis/database blacklist)

---

## ADR-013: Kubernetes over Docker Compose for Deployment

**Decision:** Add Kubernetes manifests alongside Docker Compose.

**Rationale:**
- Cloud-ready: Same YAMLs work on Minikube, EKS, GKE, AKS
- Health management: Probes auto-restart unhealthy pods
- Configuration: Secrets and ConfigMaps externalize config
- Industry standard: Most production systems use Kubernetes

**Trade-offs acknowledged:**
- Docker Compose still works for quick local development
- `imagePullPolicy: Never` required for local Minikube images
- NodePort for Gateway (external), ClusterIP for internal services

---

## ADR-014: GKE Autopilot over Standard GKE

**Decision:** Use GKE Autopilot instead of Standard mode.

**Rationale:**
- Pay-per-pod pricing (no idle node costs)
- Google manages nodes, scaling, and security patches
- Simpler for learning - focus on apps, not infrastructure

---

## ADR-015: Cloud SQL Private IP Only

**Decision:** Cloud SQL instance has no public IP, uses VPC peering.

**Rationale:**
- Database never exposed to internet
- GKE pods connect via private `10.x.x.x` address
- Requires VPC peering setup but more secure

---

## ADR-016: RabbitMQ Self-Hosted in GKE

**Decision:** Run RabbitMQ as a container in GKE (same as Minikube).

**Rationale:**
- GCP has no managed RabbitMQ service
- Alternatives (Pub/Sub, Cloud Tasks) require code changes
- Self-hosted is acceptable for learning; production would use CloudAMQP or similar

---

## ADR-017: europe-west3 Region (Frankfurt)

**Decision:** Deploy all resources in europe-west3.

**Rationale:**
- GDPR-compliant (data stays in EU)
- All resources co-located (GKE, Cloud SQL, Artifact Registry)
- Low latency for European users

---

## ADR-018: Dedicated Database User

**Decision:** Create `tradingadmin` user instead of using `postgres` superuser.

**Rationale:**
- Principle of least privilege
- If app is compromised, attacker has limited DB access
- Production best practice

---

## ADR-019: Self-Contained K8s Overlays

**Decision:** Duplicate manifests in each overlay instead of using Kustomize base references.

**Rationale:**
- Kustomize blocks `../../` references for security
- Proper base/overlay structure adds complexity for a learning project
- Each overlay is self-contained and easy to understand

**Trade-off:** Some file duplication between `k8s/` (Minikube) and `k8s/overlays/gcp/`
