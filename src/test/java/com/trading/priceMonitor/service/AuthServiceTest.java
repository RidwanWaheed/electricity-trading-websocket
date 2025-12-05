package com.trading.priceMonitor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.trading.priceMonitor.dto.AuthResult;
import com.trading.priceMonitor.entity.UserEntity;
import com.trading.priceMonitor.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserService userService;
  @Mock private JwtService jwtService;
  @InjectMocks private AuthService authService;

  private UserEntity testUser;

  @BeforeEach
  void setUp() {
    testUser = new UserEntity("trader1", "$2a$10$hashedPassword");
  }

  @Test
  void authenticate_shouldReturnTokenForValidCredentials() {
    when(userService.findByUsername("trader1")).thenReturn(Optional.of(testUser));
    when(userService.verifyPassword("correctPassword", testUser)).thenReturn(true);
    when(jwtService.generateToken("trader1")).thenReturn("jwt.token.here");

    AuthResult result = authService.authenticate("trader1", "correctPassword");

    assertTrue(result.success());
    assertEquals("jwt.token.here", result.token());
    assertEquals("trader1", result.username());
    assertNull(result.error());
  }

  @Test
  void authenticate_shouldFailForNonexistentUser() {
    when(userService.findByUsername("unknown")).thenReturn(Optional.empty());

    AuthResult result = authService.authenticate("unknown", "password");

    assertFalse(result.success());
    assertEquals("Invalid credentials", result.error());
    assertNull(result.token());
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  void authenticate_shouldFailForWrongPassword() {
    when(userService.findByUsername("trader1")).thenReturn(Optional.of(testUser));
    when(userService.verifyPassword("wrongPassword", testUser)).thenReturn(false);

    AuthResult result = authService.authenticate("trader1", "wrongPassword");

    assertFalse(result.success());
    assertEquals("Invalid credentials", result.error());
    assertNull(result.token());
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  void register_shouldCreateUserAndReturnToken() {
    UserEntity newUser = new UserEntity("newuser", "hashedPassword");
    when(userService.createUser("newuser", "password123")).thenReturn(newUser);
    when(jwtService.generateToken("newuser")).thenReturn("new.jwt.token");

    AuthResult result = authService.register("newuser", "password123");

    assertTrue(result.success());
    assertEquals("new.jwt.token", result.token());
    assertEquals("newuser", result.username());
  }

  @Test
  void register_shouldFailForDuplicateUsername() {
    when(userService.createUser("existing", "password123"))
        .thenThrow(new IllegalArgumentException("Username already exists"));

    AuthResult result = authService.register("existing", "password123");

    assertFalse(result.success());
    assertEquals("Username already exists", result.error());
    assertNull(result.token());
  }

  @Test
  void register_shouldFailForShortPassword() {
    when(userService.createUser("newuser", "12345"))
        .thenThrow(new IllegalArgumentException("Password must be at least 6 characters"));

    AuthResult result = authService.register("newuser", "12345");

    assertFalse(result.success());
    assertEquals("Password must be at least 6 characters", result.error());
    assertNull(result.token());
  }
}
