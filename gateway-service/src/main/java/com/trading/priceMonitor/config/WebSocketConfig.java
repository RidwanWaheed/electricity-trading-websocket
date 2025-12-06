package com.trading.priceMonitor.config;

import com.trading.priceMonitor.security.JwtHandshakeInterceptor;
import com.trading.priceMonitor.security.UserInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket and STOMP messaging configuration.
 *
 * <p>Configures the WebSocket infrastructure for real-time communication:
 *
 * <ul>
 *   <li>STOMP endpoints for browser connections (SockJS fallback) and native clients
 *   <li>Message broker for pub/sub messaging (/topic) and user-specific queues (/queue)
 *   <li>Heartbeat mechanism to detect stale connections (10s intervals)
 *   <li>JWT authentication via handshake interceptor
 * </ul>
 *
 * <p>Client destinations:
 *
 * <ul>
 *   <li>/topic/prices - broadcast price updates to all subscribers
 *   <li>/user/queue/order-confirmation - user-specific order status updates
 *   <li>/user/queue/errors - user-specific error messages
 *   <li>/app/order - client sends orders here (routed to OrderController)
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
  private final UserInterceptor userInterceptor;

  public WebSocketConfig(
      JwtHandshakeInterceptor jwtHandshakeInterceptor, UserInterceptor userInterceptor) {
    this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    this.userInterceptor = userInterceptor;
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config
        .enableSimpleBroker("/topic", "/queue")
        .setHeartbeatValue(new long[] {10000, 10000})
        .setTaskScheduler(heartBeatScheduler());
    config.setUserDestinationPrefix("/user");
    config.setApplicationDestinationPrefixes("/app");
  }

  @Bean
  public TaskScheduler heartBeatScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("wss-heartbeat-");
    scheduler.initialize();
    return scheduler;
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // SockJS endpoint for browsers
    registry
        .addEndpoint("/ws", "/ws-electricity")
        .addInterceptors(jwtHandshakeInterceptor)
        .setAllowedOriginPatterns("*")
        .withSockJS();

    // Plain WebSocket endpoint (for testing and native clients)
    registry
        .addEndpoint("/ws-plain")
        .addInterceptors(jwtHandshakeInterceptor)
        .setAllowedOriginPatterns("*");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(userInterceptor);
  }
}
