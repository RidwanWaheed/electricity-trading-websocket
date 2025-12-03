# Docker Guide for Spring Boot Applications

This guide explains Docker concepts used in this project, focusing on the **why** behind each decision.

---

## Table of Contents

1. [What is Docker?](#what-is-docker)
2. [Understanding the Dockerfile](#understanding-the-dockerfile)
3. [Understanding Docker Compose](#understanding-docker-compose)
4. [Commands Reference](#commands-reference)

---

## What is Docker?

Docker packages your application with all its dependencies into a **container** - a lightweight, standalone, executable package.

### The Problem Docker Solves

Without Docker:
- "Works on my machine" syndrome
- Different Java versions across environments
- Manual installation of PostgreSQL, RabbitMQ
- Configuration drift between dev/staging/production

With Docker:
- Everyone runs the exact same environment
- One command starts everything: `docker-compose up`
- No "but I have Java 17 and you have Java 21" issues

### Key Terminology

| Term | Description |
|------|-------------|
| **Image** | A snapshot of your application (like a class in OOP) |
| **Container** | A running instance of an image (like an object) |
| **Dockerfile** | Recipe for building an image |
| **docker-compose** | Orchestrates multiple containers |
| **Volume** | Persistent storage that survives container restarts |
| **Network** | How containers communicate with each other |

---

## Understanding the Dockerfile

### Why Multi-Stage Builds?

Our Dockerfile uses a **multi-stage build**. Here's why this matters:

#### Single-Stage Build (Bad)
```
┌─────────────────────────────────────────┐
│  Final Image (~750MB)                   │
│                                         │
│  - Full JDK (compiler, debugger)  400MB │
│  - Maven and build tools          100MB │
│  - Your source code (security risk!)    │
│  - All dependencies               200MB │
│  - The JAR you actually need       50MB │
└─────────────────────────────────────────┘
```

#### Multi-Stage Build (Good)
```
┌─────────────────────────────────────────┐
│  STAGE 1: Builder (thrown away)         │
│  - Full JDK, Maven, source code         │
│  - Produces: app.jar                    │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│  STAGE 2: Runtime (~250MB)              │
│  - JRE only (no compiler)         200MB │
│  - app.jar                         50MB │
│  - No source code!                      │
└─────────────────────────────────────────┘
```

**Benefits:**
- 3x smaller image (250MB vs 750MB)
- No source code in production (security)
- No build tools in production (smaller attack surface)

---

### Layer Caching Strategy

Docker builds in **layers**, and each layer is cached. The order of operations matters:

```dockerfile
# These rarely change → cached
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline -B

# This changes often → rebuilt
COPY src src
RUN ./mvnw package -DskipTests
```

**Why this order?**

| Build # | What Changed | What Rebuilds | Time |
|---------|--------------|---------------|------|
| 1st | Everything | Everything | 5 min |
| 2nd | Source code only | Only src copy + package | 30 sec |
| 3rd | pom.xml (new dependency) | Dependencies + src + package | 2 min |

If we copied everything at once, EVERY build would re-download dependencies.

---

### JDK vs JRE

| | JDK | JRE |
|---|-----|-----|
| **Contains** | Compiler (javac) + Runtime | Runtime only |
| **Size** | ~400MB | ~200MB |
| **Use case** | Building code | Running code |

**Why use JRE in production?**
- You don't compile in production
- Smaller image = faster deployments
- Smaller attack surface

---

### Security: Non-Root User

```dockerfile
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser
USER appuser
```

**Why not run as root?**

1. **Container escape attacks** - If someone exploits your app and escapes the container, they have root access to the host
2. **Principle of least privilege** - Your app doesn't need root to serve HTTP
3. **Compliance** - Many security policies require non-root containers

---

### ENTRYPOINT: Exec Form vs Shell Form

```dockerfile
# Shell form (bad for signals)
ENTRYPOINT java -jar app.jar

# Exec form (good for signals)
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Why exec form?**

| Form | How it runs | SIGTERM goes to |
|------|-------------|-----------------|
| Shell | `/bin/sh -c "java -jar..."` | The shell (not Java!) |
| Exec | Direct `java -jar...` | Java process directly |

When Docker sends SIGTERM (graceful shutdown), you want Java to receive it so Spring Boot can:
- Complete in-flight requests
- Close database connections
- Clean up resources

---

## Understanding Docker Compose

### Why Docker Compose?

To run our full stack without Docker Compose:
```bash
# Terminal 1
docker run -d --name postgres -e POSTGRES_PASSWORD=... postgres:16
# Terminal 2
docker run -d --name rabbitmq rabbitmq:3-management
# Terminal 3
docker run -d --name app --link postgres --link rabbitmq myapp
```

With Docker Compose:
```bash
docker-compose up
```

One command. One file. Done.

---

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Host (your Mac)                    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              trading-network (bridge)                │   │
│  │                                                     │   │
│  │   ┌───────────┐   ┌───────────┐   ┌───────────┐    │   │
│  │   │  app:8080 │   │postgres:  │   │rabbitmq:  │    │   │
│  │   │           │──>│   5432    │   │   5672    │    │   │
│  │   │           │──────────────────>│           │    │   │
│  │   └───────────┘   └───────────┘   └───────────┘    │   │
│  │        ↕               ↕               ↕            │   │
│  └─────────────────────────────────────────────────────┘   │
│        ↕               ↕               ↕                    │
│   Port 8080       Port 5432       Ports 5672, 15672        │
└─────────────────────────────────────────────────────────────┘
         ↕               ↕               ↕
    Your Browser    DB Tools      RabbitMQ UI
```

---

### Key Concepts Explained

#### 1. Services

A "service" is a container definition. We have three:

| Service | Image | Purpose |
|---------|-------|---------|
| `postgres` | postgres:16-alpine | Database for users & orders |
| `rabbitmq` | rabbitmq:3-management-alpine | Message broker (simulates M7) |
| `app` | Built from Dockerfile | Your Spring Boot application |

#### 2. Networks

By default, Docker containers are isolated. A **network** connects them.

```yaml
networks:
  trading-network:
    driver: bridge
```

**Key insight:** Inside the network, containers use **service names** as hostnames:

```
# From your app's perspective:
jdbc:postgresql://postgres:5432/trading_db
                   ↑
                   This resolves to the postgres container's IP
```

Not `localhost`, not an IP address - just the service name.

#### 3. Volumes

Containers are **ephemeral** - when they stop, their filesystem is gone.

```yaml
volumes:
  - postgres_data:/var/lib/postgresql/data
```

This creates a **named volume** that Docker manages. Your data survives:
- Container restarts
- Container deletion
- Image updates

**Two ways to remove:**
- `docker-compose down` - Keeps volumes (data preserved)
- `docker-compose down -v` - Deletes volumes (fresh start)

#### 4. depends_on with Health Checks

**The problem:** Your app starts, tries to connect to PostgreSQL, PostgreSQL isn't ready yet. Crash.

**The solution:**
```yaml
depends_on:
  postgres:
    condition: service_healthy
```

Docker waits until postgres's health check passes:
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U trading -d trading_db"]
  interval: 10s
  timeout: 5s
  retries: 5
```

#### 5. Environment Variables

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/trading_db
```

**Why environment variables?**
- Same image works in dev, staging, production
- Secrets aren't baked into your Docker image
- Change config without rebuilding

**Spring Boot convention:** `SPRING_DATASOURCE_URL` → `spring.datasource.url`

---

### Why These Specific Images?

| Image | Why this choice? |
|-------|------------------|
| `postgres:16-alpine` | Alpine = ~70MB vs ~400MB. Version 16 = current stable. |
| `rabbitmq:3-management-alpine` | `management` = includes web UI at :15672. Alpine = smaller. |
| `eclipse-temurin:21-jre` | Temurin = community standard JDK. JRE = no compiler needed. |

---

### Port Mappings

```yaml
ports:
  - "5432:5432"    # HOST:CONTAINER
```

| Left side | Right side |
|-----------|------------|
| Port on YOUR machine | Port INSIDE container |

You could use `"5433:5432"` if 5432 is already in use on your Mac.

---

## Commands Reference

```bash
# Start all services (detached mode)
docker-compose up -d

# Start and rebuild if Dockerfile changed
docker-compose up -d --build

# View logs (follow mode)
docker-compose logs -f

# View logs for specific service
docker-compose logs -f app

# Stop all services (keep volumes)
docker-compose down

# Stop all services and delete volumes (fresh start)
docker-compose down -v

# Restart a specific service
docker-compose restart app

# Execute command in running container
docker-compose exec postgres psql -U trading -d trading_db

# Build without starting
docker-compose build

# List running containers
docker-compose ps
```

---

## Troubleshooting

### "Port already in use"
```bash
# Find what's using port 8080
lsof -i :8080

# Kill it
kill -9 <PID>
```

### "Cannot connect to database"
1. Check if postgres is healthy: `docker-compose ps`
2. Check logs: `docker-compose logs postgres`
3. Verify environment variables match

### "App keeps restarting"
```bash
# Check app logs for errors
docker-compose logs app
```

Common causes:
- Database not ready (check depends_on)
- Wrong connection string
- Missing environment variables
