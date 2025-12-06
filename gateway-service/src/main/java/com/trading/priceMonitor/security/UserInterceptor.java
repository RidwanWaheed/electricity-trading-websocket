package com.trading.priceMonitor.security;

import java.security.Principal;
import java.util.Map;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * STOMP channel interceptor that sets the user Principal for WebSocket sessions.
 *
 * <p>This interceptor bridges the gap between HTTP handshake authentication and STOMP messaging.
 * During the handshake, JwtHandshakeInterceptor stores the username in session attributes. This
 * interceptor reads that username and creates a Principal, enabling:
 *
 * <ul>
 *   <li>User-specific message routing (/user/queue/...)
 *   <li>Access to Principal in @MessageMapping controller methods
 *   <li>Proper identification of which user sent each message
 * </ul>
 *
 * <p>Only runs on STOMP CONNECT frames (once per session).
 */
@Component
public class UserInterceptor implements ChannelInterceptor {

  /**
   * Intercepts STOMP CONNECT messages to set the user Principal.
   *
   * <p>Retrieves the username from session attributes (set by JwtHandshakeInterceptor) and creates
   * a Principal that Spring will use for the duration of the WebSocket session.
   */
  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
      Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

      if (sessionAttributes != null && sessionAttributes.containsKey("username")) {
        String username = (String) sessionAttributes.get("username");
        accessor.setUser(new StompPrincipal(username));
      }
    }
    return message;
  }

  private record StompPrincipal(String name) implements Principal {
    @Override
    public String getName() {
      return name;
    }
  }
}
