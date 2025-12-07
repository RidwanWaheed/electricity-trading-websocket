package com.trading.priceMonitor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI configuration for Swagger UI documentation. */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Electricity Trading API")
                .version("1.0")
                .description(
                    """
                    REST API for the Electricity Trading Platform.

                    ## Authentication
                    Most endpoints require JWT authentication. Use the `/api/auth/login` endpoint
                    to obtain a token, then click "Authorize" and enter: `Bearer <your-token>`

                    ## Real-time Updates
                    Price updates and order confirmations are delivered via WebSocket (STOMP over SockJS).
                    Connect to `/ws` with your JWT token for real-time data.
                    """)
                .contact(new Contact().name("Ridwan Waheed")))
        .tags(
            List.of(
                new Tag().name("Authentication").description("User login and registration"),
                new Tag().name("Balance").description("User account balance"),
                new Tag().name("Orders").description("Order history and management")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter your JWT token")));
  }
}
