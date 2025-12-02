# WebSocket Complete Guide for Java Developers

**Target Audience**: Java/Spring Boot developers familiar with REST API but new to WebSocket
**Goal**: Learn everything needed to implement Task 3.8 (Order Management REST API + WebSocket)
**Estimated Learning Time**: 4-6 hours

---

## Table of Contents

1. [WebSocket Fundamentals](#websocket-fundamentals)
2. [WebSocket vs REST - Core Concepts](#websocket-vs-rest---core-concepts)
3. [Spring WebSocket & STOMP](#spring-websocket--stomp)
4. [Hands-On Tutorial: Build Your First WebSocket](#hands-on-tutorial-build-your-first-websocket)
5. [Security & Authentication](#security--authentication)
6. [Testing WebSocket Applications](#testing-websocket-applications)
7. [Common Patterns & Best Practices](#common-patterns--best-practices)
8. [Troubleshooting & Debugging](#troubleshooting--debugging)
9. [Ready for Task 3.8](#ready-for-task-38)

---

## WebSocket Fundamentals

### What is WebSocket?

**Simple Definition**: WebSocket is a **persistent, two-way connection** between client and server that allows **real-time communication**.

**Key Differences from HTTP/REST**:

| Feature | HTTP/REST | WebSocket |
|---------|-----------|-----------|
| **Connection** | Open → Request → Response → Close | Open once → Keep alive → Send messages both ways |
| **Direction** | Client → Server only | Client ↔ Server (bidirectional) |
| **Latency** | New connection each time (~100ms overhead) | Messages sent instantly (~1-5ms) |
| **Use Case** | Commands, queries | Real-time updates, notifications |
| **Overhead** | Full HTTP headers each request (~500 bytes) | Minimal frame (~2-6 bytes per message) |

### Real-World Analogy

**HTTP/REST = Postal Mail**
- You write a letter (HTTP request)
- Mail carrier takes it to destination
- They write back (HTTP response)
- Mail carrier brings response
- **For each message**: New round trip

**WebSocket = Phone Call**
- You call once (establish WebSocket connection)
- Line stays open
- Both parties can talk anytime
- No need to "reconnect" for each sentence
- **Efficient for conversations**

### When to Use WebSocket

✅ **Use WebSocket when:**
- Real-time updates needed (chat, notifications, live data)
- High-frequency updates (order book, stock prices)
- Server needs to push data without client asking
- Low latency critical (<100ms)
- Many updates from server to client

❌ **Don't use WebSocket when:**
- Simple request-response (use REST)
- Infrequent operations (use REST)
- Caching needed (use REST with Cache-Control)
- Need HTTP features (status codes, redirects, etc.)

---

## WebSocket vs REST - Core Concepts

### HTTP/REST Pattern

```
Client                                Server
  |                                      |
  |------ GET /api/orders/123 --------->|
  |                                      | (process request)
  |<----- 200 OK { order data } --------|
  |                                      |
  | (connection closed)                 |
  |                                      |
  |------ GET /api/orders/123 --------->| (3 seconds later)
  |                                      | (process request)
  |<----- 200 OK { order data } --------|
  |                                      |
  | (connection closed)                 |

Problem: Client must poll repeatedly to get updates
Result: Latency, wasted bandwidth, server load
```

### WebSocket Pattern

```
Client                                Server
  |                                      |
  |------ WebSocket Handshake --------->|
  |<----- 101 Switching Protocols ------|
  |                                      |
  | ===== Connection established ====== |
  |                                      |
  |                                      | (order executed)
  |<----- { order update } -------------|
  |                                      |
  |                                      | (another execution)
  |<----- { order update } -------------|
  |                                      |
  | (connection stays open)              |

Benefit: Server pushes updates instantly
Result: Low latency, efficient, real-time
```

---

## Spring WebSocket & STOMP

### WebSocket Protocol Layers

Spring WebSocket uses a layered approach:

```
┌─────────────────────────────────────┐
│   Your Application Code             │ ← Your @MessageMapping, @SendTo
├─────────────────────────────────────┤
│   STOMP Protocol                    │ ← Message format, subscriptions
├─────────────────────────────────────┤
│   WebSocket Protocol                │ ← Persistent connection
├─────────────────────────────────────┤
│   TCP/IP                            │ ← Network transport
└─────────────────────────────────────┘
```

### What is STOMP?

**STOMP** = Simple Text Oriented Messaging Protocol

**Why STOMP?**
- Raw WebSocket only sends/receives strings
- STOMP adds structure: destinations, subscriptions, message types
- Think of it as "HTTP for WebSocket" (adds conventions)

**STOMP Message Format**:
```
SEND
destination:/app/hello
content-type:application/json

{"message": "Hello World"}
```

### Key STOMP Concepts

#### 1. Destinations (Like URLs in REST)

```java
// REST API
GET /api/orders/123           ← URL path

// STOMP WebSocket
destination: /topic/orders/123  ← Destination path
```

**Destination Prefixes**:
- `/topic/...` - Publish-subscribe (one-to-many)
  - Example: `/topic/market/events` - All users receive market HALT event
- `/queue/...` - Point-to-point (one-to-one)
  - Example: `/queue/orders/user123` - Only user123 receives their orders
- `/app/...` - Client sends to server
  - Example: `/app/orders/submit` - Client sends order submission

#### 2. Subscribe (Like GET in REST)

```javascript
// REST API - Client requests data
GET /api/orders/active

// STOMP WebSocket - Client subscribes to updates
stompClient.subscribe('/topic/orders/user123', (message) => {
  console.log('Received:', message.body);
});
```

**Key Difference**:
- REST: Client asks once, gets one response
- WebSocket: Client subscribes once, receives many messages

#### 3. Send (Like POST in REST)

```javascript
// REST API - Client sends command
POST /api/orders/entry
Body: { order details }

// STOMP WebSocket - Client sends message (NOT RECOMMENDED for commands)
stompClient.send('/app/orders/submit', {}, JSON.stringify(order));
```

**Important**: For commands (submit, modify, cancel), use REST API!
WebSocket is for server → client notifications.

---

## Hands-On Tutorial: Build Your First WebSocket

### Step 1: Add Dependencies

**File**: `pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Starter WebSocket -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <!-- Spring Boot Starter Web (for REST + WebSocket) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### Step 2: Configure WebSocket

**File**: `src/main/java/com/example/config/WebSocketConfig.java`

```java
package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker  // Enable WebSocket with STOMP
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for /topic destinations
        config.enableSimpleBroker("/topic");

        // Set prefix for messages from client to server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint that clients connect to
        registry.addEndpoint("/ws")           // WebSocket URL: ws://localhost:8080/ws
                .setAllowedOrigins("*")        // Allow all origins (for development)
                .withSockJS();                 // Enable SockJS fallback for older browsers
    }
}
```

**What this does**:
- `/ws` - WebSocket connection endpoint (like a REST API base URL)
- `/topic/*` - Destinations for publish-subscribe messages
- `/app/*` - Prefix for messages client sends to server
- `withSockJS()` - Fallback for browsers without WebSocket support

### Step 3: Create a Message Model

**File**: `src/main/java/com/example/model/Notification.java`

```java
package com.example.model;

import java.time.Instant;

public class Notification {
    private String message;
    private Instant timestamp;
    private String type;  // INFO, WARNING, ERROR

    // Constructors
    public Notification() {
    }

    public Notification(String message, String type) {
        this.message = message;
        this.type = type;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
```

### Step 4: Create REST Controller to Trigger Notifications

**File**: `src/main/java/com/example/controller/NotificationController.java`

```java
package com.example.controller;

import com.example.model.Notification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    // Inject SimpMessagingTemplate to send WebSocket messages
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * REST endpoint to trigger a notification
     *
     * Call this: POST http://localhost:8080/api/notifications/send
     * Body: { "message": "Hello WebSocket", "type": "INFO" }
     */
    @PostMapping("/send")
    public String sendNotification(@RequestBody Notification notification) {

        // Send notification to WebSocket topic
        messagingTemplate.convertAndSend(
            "/topic/notifications",   // Destination (subscribers will receive this)
            notification              // Message to send
        );

        return "Notification sent to WebSocket subscribers!";
    }

    /**
     * REST endpoint to trigger a user-specific notification
     *
     * Call this: POST http://localhost:8080/api/notifications/send/user123
     * Body: { "message": "Hello user123", "type": "INFO" }
     */
    @PostMapping("/send/{userId}")
    public String sendUserNotification(
            @PathVariable String userId,
            @RequestBody Notification notification) {

        // Send notification to specific user
        messagingTemplate.convertAndSend(
            "/topic/notifications/" + userId,  // User-specific destination
            notification
        );

        return "Notification sent to user " + userId;
    }
}
```

**Key Concept**: `SimpMessagingTemplate`
- This is how you send WebSocket messages from server-side Java code
- Similar to `RestTemplate` for REST APIs
- `convertAndSend(destination, message)` - Sends message to WebSocket topic

### Step 5: Create Frontend WebSocket Client

**File**: `src/main/resources/static/websocket-test.html`

```html
<!DOCTYPE html>
<html>
<head>
    <title>WebSocket Test</title>
    <!-- Include SockJS and STOMP libraries -->
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/dist/stomp.umd.min.js"></script>
</head>
<body>
    <h1>WebSocket Test</h1>

    <div>
        <h2>Connection Status: <span id="status">Disconnected</span></h2>
    </div>

    <div>
        <h2>Received Notifications:</h2>
        <ul id="notifications"></ul>
    </div>

    <div>
        <h2>Send Notification (via REST API)</h2>
        <input type="text" id="message" placeholder="Enter message" />
        <button onclick="sendNotification()">Send to All</button>
        <button onclick="sendUserNotification()">Send to user123</button>
    </div>

    <script>
        // Create STOMP client
        const stompClient = new StompJs.Client({
            // WebSocket broker URL
            brokerURL: 'ws://localhost:8080/ws',

            // If brokerURL doesn't work, use SockJS fallback
            webSocketFactory: () => new SockJS('http://localhost:8080/ws'),

            // Debug logging
            debug: (str) => {
                console.log('STOMP: ' + str);
            },

            // Reconnect automatically if connection drops
            reconnectDelay: 5000,

            // Connection established
            onConnect: (frame) => {
                console.log('Connected:', frame);
                document.getElementById('status').textContent = 'Connected';
                document.getElementById('status').style.color = 'green';

                // Subscribe to global notifications
                stompClient.subscribe('/topic/notifications', (message) => {
                    console.log('Received notification:', message.body);
                    const notification = JSON.parse(message.body);
                    displayNotification(notification);
                });

                // Subscribe to user-specific notifications
                stompClient.subscribe('/topic/notifications/user123', (message) => {
                    console.log('Received user notification:', message.body);
                    const notification = JSON.parse(message.body);
                    displayNotification(notification, 'User-specific: ');
                });
            },

            // Connection error
            onStompError: (frame) => {
                console.error('STOMP error:', frame);
                document.getElementById('status').textContent = 'Error';
                document.getElementById('status').style.color = 'red';
            },

            // WebSocket error
            onWebSocketError: (event) => {
                console.error('WebSocket error:', event);
            },

            // Connection closed
            onDisconnect: () => {
                console.log('Disconnected');
                document.getElementById('status').textContent = 'Disconnected';
                document.getElementById('status').style.color = 'gray';
            }
        });

        // Activate (connect) the STOMP client
        stompClient.activate();

        // Display notification in UI
        function displayNotification(notification, prefix = '') {
            const ul = document.getElementById('notifications');
            const li = document.createElement('li');
            const timestamp = new Date(notification.timestamp).toLocaleTimeString();
            li.textContent = `${prefix}[${timestamp}] ${notification.type}: ${notification.message}`;
            li.style.color = notification.type === 'ERROR' ? 'red' :
                             notification.type === 'WARNING' ? 'orange' : 'black';
            ul.insertBefore(li, ul.firstChild);
        }

        // Send notification via REST API (which triggers WebSocket push)
        async function sendNotification() {
            const message = document.getElementById('message').value;
            if (!message) {
                alert('Please enter a message');
                return;
            }

            const response = await fetch('http://localhost:8080/api/notifications/send', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: message, type: 'INFO' })
            });

            const result = await response.text();
            console.log(result);
        }

        // Send user-specific notification via REST API
        async function sendUserNotification() {
            const message = document.getElementById('message').value;
            if (!message) {
                alert('Please enter a message');
                return;
            }

            const response = await fetch('http://localhost:8080/api/notifications/send/user123', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: message, type: 'INFO' })
            });

            const result = await response.text();
            console.log(result);
        }

        // Cleanup on page unload
        window.addEventListener('beforeunload', () => {
            stompClient.deactivate();
        });
    </script>
</body>
</html>
```

### Step 6: Run and Test

#### Start the Application

```bash
# Terminal 1: Start Spring Boot application
cd apps/project-m7/m7-trading-engine
mvn spring-boot:run
```

#### Test WebSocket Connection

1. **Open browser**: `http://localhost:8080/websocket-test.html`
2. **Check connection status**: Should say "Connected" in green
3. **Type a message**: Enter "Hello WebSocket"
4. **Click "Send to All"**: Message appears in notifications list
5. **Click "Send to user123"**: Message appears with "User-specific:" prefix

#### What's Happening Behind the Scenes

```
1. Browser opens websocket-test.html
   ↓
2. JavaScript creates STOMP client and connects to ws://localhost:8080/ws
   ↓
3. Server accepts WebSocket connection (HTTP 101 Switching Protocols)
   ↓
4. JavaScript subscribes to /topic/notifications
   ↓
5. User clicks "Send to All" button
   ↓
6. JavaScript calls REST API: POST /api/notifications/send
   ↓
7. NotificationController receives REST request
   ↓
8. NotificationController calls messagingTemplate.convertAndSend()
   ↓
9. Spring sends message to /topic/notifications
   ↓
10. All subscribers receive message (including the browser)
   ↓
11. JavaScript onMessage callback displays notification
```

---

## Security & Authentication

### Problem: Anyone Can Connect to WebSocket

**Without security**:
```java
registry.addEndpoint("/ws").setAllowedOrigins("*");
// Anyone can connect! No authentication!
```

### Solution: JWT Authentication for WebSocket

#### Step 1: Configure WebSocket Security

**File**: `src/main/java/com/example/config/WebSocketSecurityConfig.java`

```java
package com.example.config;

import com.example.security.JwtTokenService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

@Configuration
public class WebSocketSecurityConfig {

    private final JwtTokenService jwtTokenService;

    public WebSocketSecurityConfig(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * Channel interceptor to authenticate WebSocket connections
     */
    public ChannelInterceptor authChannelInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // Only authenticate on CONNECT command
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {

                    // Extract JWT token from STOMP headers
                    String token = accessor.getFirstNativeHeader("Authorization");

                    if (token != null && token.startsWith("Bearer ")) {
                        String jwt = token.substring(7);

                        try {
                            // Validate JWT token
                            String username = jwtTokenService.validateToken(jwt);

                            // Create authentication object
                            Authentication auth = new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );

                            // Set authentication in STOMP session
                            accessor.setUser(auth);

                        } catch (Exception e) {
                            // Invalid token - reject connection
                            throw new IllegalArgumentException("Invalid JWT token");
                        }
                    } else {
                        // No token provided - reject connection
                        throw new IllegalArgumentException("Missing JWT token");
                    }
                }

                return message;
            }
        };
    }
}
```

#### Step 2: Register Interceptor

**File**: Modify `WebSocketConfig.java`

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketSecurityConfig securityConfig;

    public WebSocketConfig(WebSocketSecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register authentication interceptor
        registration.interceptors(securityConfig.authChannelInterceptor());
    }

    // ... rest of configuration
}
```

#### Step 3: Frontend - Send JWT Token on Connect

**File**: Modify `websocket-test.html`

```javascript
// Assume you have JWT token from login
const jwtToken = localStorage.getItem('jwt_token');

const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws',

    // Send JWT token in CONNECT headers
    connectHeaders: {
        Authorization: `Bearer ${jwtToken}`
    },

    onConnect: (frame) => {
        console.log('Connected with authentication:', frame);
        // ... subscribe to topics
    },

    onStompError: (frame) => {
        console.error('Authentication failed:', frame.headers['message']);
        // Redirect to login page
    }
});

stompClient.activate();
```

### User-Specific Destinations

**Problem**: How to send messages to specific users only?

**Solution**: Use `/user/{username}/...` destinations

#### Backend: Send to Specific User

```java
@Service
public class OrderService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyUserAboutOrder(String userId, OrderUpdate update) {
        // Send to specific user
        messagingTemplate.convertAndSendToUser(
            userId,                      // Username (from JWT)
            "/topic/orders",             // Destination
            update                       // Message
        );

        // Spring automatically converts to: /user/{userId}/topic/orders
    }
}
```

#### Frontend: Subscribe to User-Specific Topic

```javascript
stompClient.subscribe('/user/topic/orders', (message) => {
    // Only this user receives these messages
    const orderUpdate = JSON.parse(message.body);
    console.log('My order update:', orderUpdate);
});
```

---

## Testing WebSocket Applications

### Unit Testing WebSocket Controllers

**File**: `src/test/java/com/example/controller/NotificationControllerTest.java`

```java
package com.example.controller;

import com.example.model.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
class NotificationControllerTest {

    @Autowired
    private NotificationController controller;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void testSendNotification() {
        // Arrange
        Notification notification = new Notification("Test message", "INFO");

        // Act
        controller.sendNotification(notification);

        // Assert
        verify(messagingTemplate).convertAndSend(
            eq("/topic/notifications"),
            eq(notification)
        );
    }

    @Test
    void testSendUserNotification() {
        // Arrange
        String userId = "user123";
        Notification notification = new Notification("Test message", "INFO");

        // Act
        controller.sendUserNotification(userId, notification);

        // Assert
        verify(messagingTemplate).convertAndSend(
            eq("/topic/notifications/" + userId),
            eq(notification)
        );
    }
}
```

### Integration Testing WebSocket

**File**: `src/test/java/com/example/WebSocketIntegrationTest.java`

```java
package com.example;

import com.example.model.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void testWebSocketConnection() throws Exception {
        // Create WebSocket client
        WebSocketStompClient stompClient = new WebSocketStompClient(
            new StandardWebSocketClient()
        );
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // Connect to WebSocket
        String wsUrl = "ws://localhost:" + port + "/ws";
        CompletableFuture<Notification> receivedMessage = new CompletableFuture<>();

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                // Subscribe to topic
                session.subscribe("/topic/notifications", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return Notification.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        receivedMessage.complete((Notification) payload);
                    }
                });

                // Send test message
                Notification testNotification = new Notification("Test", "INFO");
                session.send("/app/notifications", testNotification);
            }
        };

        stompClient.connectAsync(wsUrl, sessionHandler).get(5, TimeUnit.SECONDS);

        // Wait for message
        Notification received = receivedMessage.get(5, TimeUnit.SECONDS);

        // Assert
        assertNotNull(received);
        assertEquals("Test", received.getMessage());
        assertEquals("INFO", received.getType());
    }
}
```

### Manual Testing with Browser Dev Tools

#### Open Browser Console (F12)

```javascript
// Test WebSocket connection manually
const socket = new WebSocket('ws://localhost:8080/ws');

socket.onopen = () => {
    console.log('WebSocket connected');

    // Send STOMP CONNECT frame
    socket.send('CONNECT\nAccept-version:1.1,1.0\n\n\x00');
};

socket.onmessage = (event) => {
    console.log('Received:', event.data);
};

socket.onerror = (error) => {
    console.error('WebSocket error:', error);
};

socket.onclose = () => {
    console.log('WebSocket closed');
};
```

### Testing with Postman (REST Trigger)

1. **Trigger notification via REST**:
   - Method: POST
   - URL: `http://localhost:8080/api/notifications/send`
   - Body: `{ "message": "Test from Postman", "type": "INFO" }`

2. **Check WebSocket client receives message**:
   - Open `websocket-test.html` in browser
   - Click "Send" in Postman
   - Notification should appear in browser

---

## Common Patterns & Best Practices

### Pattern 1: REST for Commands, WebSocket for Events

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;

    // REST API: User submits order (command)
    @PostMapping("/entry")
    public ResponseEntity<OrderResponse> submitOrder(@RequestBody OrderRequest request) {
        // Validate and submit order
        Order order = orderService.submitOrder(request);

        // Return synchronous response
        return ResponseEntity.ok(new OrderResponse(order.getId(), "submitted"));
    }
}

@Service
public class OrderExecutionReportProcessor {

    private final SimpMessagingTemplate messagingTemplate;

    // Process M7 execution report
    public void processExecutionReport(ExecutionReport report) {
        // Update order state
        Order order = updateOrderFromReport(report);

        // Push update to WebSocket (event)
        messagingTemplate.convertAndSendToUser(
            order.getUserId(),
            "/topic/orders",
            order
        );
    }
}
```

**Key Principle**: Commands via REST, notifications via WebSocket

### Pattern 2: Subscription Management

```java
@Configuration
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String username = headers.getUser().getName();

        log.info("User connected: {}", username);

        // Track active WebSocket sessions
        // Useful for monitoring and cleanup
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String username = headers.getUser().getName();

        log.info("User disconnected: {}", username);

        // Cleanup resources associated with user session
    }
}
```

### Pattern 3: Broadcast vs User-Specific Messages

```java
@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // Broadcast to ALL users (market HALT event)
    public void sendMarketHaltNotification() {
        MarketEvent event = new MarketEvent("MARKET_HALT", "Market halted");

        messagingTemplate.convertAndSend(
            "/topic/market/events",  // All subscribers receive this
            event
        );
    }

    // Send to SPECIFIC user (order execution)
    public void sendOrderExecutionNotification(String userId, Order order) {
        OrderUpdate update = new OrderUpdate(order);

        messagingTemplate.convertAndSendToUser(
            userId,                  // Only this user receives
            "/topic/orders",
            update
        );
    }

    // Send to SPECIFIC contract subscribers (order book update)
    public void sendOrderBookUpdate(String contractId, OrderBook orderBook) {
        messagingTemplate.convertAndSend(
            "/topic/market/orderbook/" + contractId,  // All subscribed to this contract
            orderBook
        );
    }
}
```

### Pattern 4: Error Handling

```java
@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");

        // Configure error destination
        config.setUserDestinationPrefix("/user");
    }

    @Bean
    public StompSubProtocolErrorHandler stompSubProtocolErrorHandler() {
        return new StompSubProtocolErrorHandler() {
            @Override
            public Message<byte[]> handleClientMessageProcessingError(
                    Message<byte[]> clientMessage, Throwable ex) {

                // Log error
                log.error("Error processing WebSocket message", ex);

                // Return error message to client
                return super.handleClientMessageProcessingError(clientMessage, ex);
            }
        };
    }
}
```

### Pattern 5: Health Checks

```java
@Component
public class WebSocketHealthIndicator implements HealthIndicator {

    private final ApplicationContext applicationContext;
    private volatile int activeConnections = 0;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        activeConnections++;
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        activeConnections--;
    }

    @Override
    public Health health() {
        if (activeConnections > 0) {
            return Health.up()
                .withDetail("activeConnections", activeConnections)
                .build();
        } else {
            // During trading hours, no connections is a problem
            return Health.down()
                .withDetail("activeConnections", 0)
                .withDetail("reason", "No active WebSocket connections")
                .build();
        }
    }
}
```

---

## Troubleshooting & Debugging

### Common Issues

#### Issue 1: WebSocket Connection Refused

**Error**: `WebSocket connection to 'ws://localhost:8080/ws' failed`

**Causes**:
1. Spring Boot not running
2. Wrong WebSocket URL
3. Firewall blocking WebSocket
4. Port already in use

**Solutions**:
```bash
# Check if Spring Boot is running
curl http://localhost:8080/actuator/health

# Check if port 8080 is in use
lsof -i :8080

# Try SockJS fallback
const socket = new SockJS('http://localhost:8080/ws');
```

#### Issue 2: CORS Issues

**Error**: `Access to WebSocket at 'ws://...' from origin 'http://...' has been blocked by CORS policy`

**Solution**:
```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")  // Allow all origins (development)
            .withSockJS();
}
```

#### Issue 3: Messages Not Received

**Possible Causes**:
1. Not subscribed to correct destination
2. Subscription happened after message sent
3. User not authenticated (for user-specific messages)

**Debug**:
```javascript
stompClient.debug = (str) => {
    console.log('STOMP DEBUG:', str);  // Enable verbose logging
};

stompClient.subscribe('/topic/orders', (message) => {
    console.log('RAW MESSAGE:', message);  // Log raw message
    console.log('BODY:', message.body);    // Log body
});
```

#### Issue 4: WebSocket Closes Immediately

**Error**: Connection opens then closes immediately

**Causes**:
1. Authentication failed
2. Server rejected connection
3. Protocol mismatch

**Debug**:
```java
@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Enable detailed logging
        registration.setMessageSizeLimit(512 * 1024);
        registration.setSendBufferSizeLimit(1024 * 1024);
        registration.setSendTimeLimit(20000);
    }
}
```

```properties
# application.yml - Enable debug logging
logging:
  level:
    org.springframework.messaging: DEBUG
    org.springframework.web.socket: DEBUG
```

### Debugging Tools

#### Browser Developer Tools

**Chrome/Firefox DevTools → Network Tab → WS (WebSocket)**
- Shows WebSocket connection
- Displays all frames (messages) sent/received
- Shows connection status

#### Spring Boot Actuator

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```properties
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,websocket
```

#### WebSocket Test Clients

**Online Tools**:
- https://websocket.org/echo.html
- https://www.piesocket.com/websocket-tester

**Desktop Tools**:
- Postman (supports WebSocket)
- wscat (command-line)

```bash
# Install wscat
npm install -g wscat

# Test WebSocket connection
wscat -c ws://localhost:8080/ws
```

---

## Ready for Task 3.8

### What You Should Know Now

✅ **WebSocket Fundamentals**
- What WebSocket is and when to use it
- How it differs from REST API
- Persistent bidirectional connection concept

✅ **Spring WebSocket & STOMP**
- How to configure WebSocket in Spring Boot
- STOMP protocol basics (destinations, subscriptions)
- `SimpMessagingTemplate` for sending messages

✅ **Security**
- JWT authentication for WebSocket
- User-specific vs broadcast messages
- Channel interceptors

✅ **Testing**
- Unit testing with mocked `SimpMessagingTemplate`
- Integration testing with WebSocket client
- Manual testing with browser

✅ **Patterns**
- REST for commands, WebSocket for events
- Broadcast vs user-specific messaging
- Error handling and health checks

### Task 3.8 Checklist

When implementing Task 3.8, you'll need to:

#### Backend (Java/Spring Boot)

- [ ] Configure WebSocket with STOMP (`WebSocketConfig.java`)
- [ ] Add JWT authentication interceptor (`WebSocketSecurityConfig.java`)
- [ ] Inject `SimpMessagingTemplate` into processors
- [ ] Push order updates to `/topic/orders/{userId}` after processing execution reports
- [ ] Push execution notifications to `/topic/executions/{userId}` when trades occur
- [ ] Push OMT warnings to `/topic/market/omt` when thresholds crossed
- [ ] Add WebSocket health checks and metrics
- [ ] Write unit tests for WebSocket message sending

#### Frontend (React - Task 3.9)

- [ ] Install `@stomp/stompjs` and `sockjs-client`
- [ ] Create WebSocket hook (`useOrderWebSocket`)
- [ ] Connect with JWT token in headers
- [ ] Subscribe to user-specific topics
- [ ] Apply order updates to React state
- [ ] Display execution notifications (toast/alert)
- [ ] Show OMT warnings in header banner
- [ ] Handle reconnection on disconnect

### Key Files You'll Create/Modify

**Backend**:
```
shared/config/WebSocketConfig.java          (NEW)
shared/config/WebSocketSecurityConfig.java  (NEW)
orders/broadcast/OrderExecutionReportProcessor.java  (MODIFY - add WebSocket push)
orders/throttling/OMTTracker.java           (MODIFY - add WebSocket push)
orders/monitoring/WebSocketHealthIndicator.java  (NEW)
```

**Frontend (Task 3.9)**:
```
src/hooks/useOrderWebSocket.ts              (NEW)
src/hooks/useExecutionWebSocket.ts          (NEW)
src/hooks/useOMTStatus.ts                   (NEW)
src/components/OrderBlotter.tsx             (MODIFY - use WebSocket)
```

### Reference Code for Task 3.8

**WebSocket Configuration** (copy-paste ready):

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/trading")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

**Sending WebSocket Message** (in OrderExecutionReportProcessor):

```java
@Service
public class OrderExecutionReportProcessor {

    private final SimpMessagingTemplate messagingTemplate;

    public void processExecutionReport(OrdrExeRprt report) {
        // Existing logic to process report
        OrderInfo orderInfo = processReport(report);

        // NEW: Push to WebSocket
        OrderStatusResponse response = OrderStatusResponse.fromOrderInfo(orderInfo);
        messagingTemplate.convertAndSendToUser(
            orderInfo.userId(),
            "/topic/orders",
            response
        );
    }
}
```

---

## Additional Resources

### Official Documentation
- **Spring WebSocket**: https://docs.spring.io/spring-framework/reference/web/websocket.html
- **STOMP Protocol**: https://stomp.github.io/
- **WebSocket RFC**: https://tools.ietf.org/html/rfc6455

### Video Tutorials
- **Spring WebSocket Tutorial**: https://www.youtube.com/results?search_query=spring+boot+websocket+tutorial
- **STOMP.js Guide**: https://stomp-js.github.io/guide/stompjs/using-stompjs-v5.html

### Practice Projects
1. **Simple Chat App**: Build a chat room with WebSocket
2. **Live Dashboard**: Build a dashboard that updates in real-time
3. **Notification System**: Build a notification system (like our tutorial)

### Books
- **"Spring in Action" (6th Edition)** - Chapter on WebSocket
- **"Pro Spring 5"** - WebSocket section

---

## Summary

**What is WebSocket?**
- Persistent, bidirectional connection between client and server
- Use for real-time updates, server-push notifications
- More efficient than polling for high-frequency updates

**When to use WebSocket?**
- ✅ Real-time notifications (order executions, market events)
- ✅ High-frequency updates (order books, live prices)
- ✅ Server needs to push without client asking
- ❌ Don't use for commands (use REST API)

**Key Spring Components**:
- `@EnableWebSocketMessageBroker` - Enable WebSocket
- `WebSocketMessageBrokerConfigurer` - Configure endpoints and broker
- `SimpMessagingTemplate` - Send messages from server
- `@SubscribeMapping` / `@MessageMapping` - Handle client messages (rarely used)

**Key Frontend Components**:
- STOMP.js `Client` - Connect to WebSocket
- `subscribe()` - Listen for messages
- `send()` - Send messages to server (rarely used for commands)

**Security**:
- Use JWT in `connectHeaders` for authentication
- Validate JWT in `ChannelInterceptor` on server
- Use `/user/{username}/...` for user-specific messages

**Testing**:
- Unit tests: Mock `SimpMessagingTemplate`
- Integration tests: Use `WebSocketStompClient`
- Manual tests: Browser DevTools Network tab (WS)

---

## Next Steps

### Today (Learning Day)

1. **Run the hands-on tutorial** (2 hours)
   - Create the NotificationController example
   - Test with browser client
   - Observe messages in browser DevTools

2. **Experiment with variations** (1 hour)
   - Try broadcast messages
   - Try user-specific messages
   - Break things and see what errors you get

3. **Review Task 3.8 code** (1 hour)
   - Read `OrderExecutionReportProcessor.java`
   - Identify where to add `messagingTemplate.convertAndSendToUser()`
   - Plan WebSocket integration points

4. **Watch a video tutorial** (1 hour)
   - Search "Spring Boot WebSocket Tutorial" on YouTube
   - Follow along with code

### Tomorrow (Implementation Day)

1. **Start Task 3.8.1-3.8.7** (REST API endpoints)
   - These you're already familiar with!

2. **Implement Task 3.8.8** (WebSocket)
   - Create `WebSocketConfig.java` (copy from tutorial)
   - Add `messagingTemplate` to processors
   - Push order updates after processing execution reports
   - Test with browser client

3. **Test end-to-end**
   - Submit order via REST API
   - Receive execution report from M7
   - See WebSocket push to frontend
   - Verify frontend updates

---

**Good luck with your learning!** 🚀

Take your time with the hands-on tutorial - actually typing the code and seeing it work is the best way to learn WebSocket. Tomorrow you'll be ready to implement Task 3.8 with confidence.
