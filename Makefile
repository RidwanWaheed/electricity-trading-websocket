.PHONY: up down run test fmt check build logs clean deps

# Start all Docker services
up:
	docker-compose up -d

# Stop all Docker services
down:
	docker-compose down

# Start only dependencies (postgres + rabbitmq)
deps:
	docker-compose up -d postgres rabbitmq

# Run app locally (requires deps)
run:
	./mvnw spring-boot:run

# Run unit tests
test:
	./mvnw test

# Run all tests including integration
test-all:
	./mvnw test -DexcludedGroups=

# Format code
fmt:
	./mvnw spotless:apply

# Check code formatting
check:
	./mvnw spotless:check

# Build JAR (skip tests)
build:
	./mvnw package -DskipTests

# Tail app logs
logs:
	docker-compose logs -f app

# Clean build artifacts
clean:
	./mvnw clean

# Rebuild and restart app container
rebuild:
	docker-compose up -d --build app
