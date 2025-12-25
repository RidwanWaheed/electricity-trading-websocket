.PHONY: up down deps run-gateway run-order run-m7 test fmt check build logs clean rebuild

COMPOSE := docker-compose -f docker/docker-compose.yml

# =============================================================================
# Docker Commands
# =============================================================================

# Start all services (postgres, rabbitmq, gateway, order-service, mock-m7)
up:
	$(COMPOSE) up -d

# Stop all services
down:
	$(COMPOSE) down

# Start only infrastructure (postgres + rabbitmq) for local development
deps:
	$(COMPOSE) up -d postgres rabbitmq

# Rebuild and restart all app containers
rebuild:
	$(COMPOSE) up -d --build gateway order-service mock-m7

# =============================================================================
# Local Development (requires: make deps)
# =============================================================================

# Run Gateway locally
run-gateway:
	./mvnw spring-boot:run -pl gateway-service

# Run Order Service locally
run-order:
	./mvnw spring-boot:run -pl order-service

# Run Mock M7 locally
run-m7:
	./mvnw spring-boot:run -pl mock-m7-service

# Run all services locally (in background) - requires 3 terminals or use &
run-all:
	@echo "Run each service in a separate terminal:"
	@echo "  make run-gateway"
	@echo "  make run-order"
	@echo "  make run-m7"

# =============================================================================
# Build & Test
# =============================================================================

# Build all modules (skip tests)
build:
	./mvnw package -DskipTests

# Run unit tests for all modules
test:
	./mvnw test

# Run all tests including integration
test-all:
	./mvnw test -DexcludedGroups=

# Format code (all modules)
fmt:
	./mvnw spotless:apply

# Check code formatting
check:
	./mvnw spotless:check

# Clean build artifacts
clean:
	./mvnw clean

# =============================================================================
# Logs
# =============================================================================

# Tail all service logs
logs:
	$(COMPOSE) logs -f gateway order-service mock-m7

# Tail specific service logs
logs-gateway:
	$(COMPOSE) logs -f gateway

logs-order:
	$(COMPOSE) logs -f order-service

logs-m7:
	$(COMPOSE) logs -f mock-m7

# =============================================================================
# Utilities
# =============================================================================

# Check health of all services
health:
	@echo "Gateway:       $$(curl -s http://localhost:8080/actuator/health | head -c 50)"
	@echo "Order Service: $$(curl -s http://localhost:8081/actuator/health | head -c 50)"
	@echo "Mock M7:       $$(curl -s http://localhost:8082/actuator/health | head -c 50)"

# Open RabbitMQ Management UI
rabbitmq:
	open http://localhost:15672
