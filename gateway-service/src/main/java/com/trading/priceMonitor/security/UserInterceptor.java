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

@Component
public class UserInterceptor implements ChannelInterceptor {

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
