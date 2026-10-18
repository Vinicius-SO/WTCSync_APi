package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.dtos.AuthRequestDTO;
import com.fiap.WtcSync.application.dtos.UsuarioRequestDTO;
import com.fiap.WtcSync.application.services.TokenService;
import com.fiap.WtcSync.domain.entities.User;
import com.fiap.WtcSync.domain.interfaces.IUserRepository;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {
    IUserRepository users = mock(IUserRepository.class);
    TokenService tokens = mock(TokenService.class);
    PasswordEncoder passwords = mock(PasswordEncoder.class);
    AuthController controller = new AuthController(tokens, passwords, users);
    User user;

    @BeforeEach void setup() {
        user = new User("Test User", "test@example.com", "hash", "OPERATOR");
        user.setId("user-id");
    }
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void validPasswordReturnsBackendToken() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwords.matches("password", "hash")).thenReturn(true);
        when(tokens.generateToken(user.getEmail())).thenReturn("jwt");
        var response = controller.login(new AuthRequestDTO(" test@example.com ", "password"));
        assertEquals(200, response.getStatusCode().value());
        assertEquals("jwt", response.getBody().token());
    }
    @Test void inactiveUserCannotLogin() {
        user.setActive(false);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwords.matches("password", "hash")).thenReturn(true);
        assertEquals(401, controller.login(new AuthRequestDTO(user.getEmail(), "password")).getStatusCode().value());
        verifyNoInteractions(tokens);
    }
    @Test void incompleteCredentialsReturnBadRequest() {
        assertEquals(400, controller.login(new AuthRequestDTO(null, "password")).getStatusCode().value());
        verifyNoInteractions(users);
    }
    @Test void profileIncludesTrustedRoleAndIdentity() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var response = controller.getCurrentUser();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("OPERATOR", response.getBody().get("role"));
        assertEquals("user-id", response.getBody().get("id"));
    }
    @Test void inactiveProfileIsRejected() {
        user.setActive(false);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        assertEquals(401, controller.getCurrentUser().getStatusCode().value());
    }
    @Test void registrationUsesApiStoreAndCannotGrantOperatorRole() {
        when(passwords.encode("password")).thenReturn("encoded");
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            assertEquals("CLIENT", saved.getRole());
            assertEquals("encoded", saved.getPassword());
            assertEquals("Test User", saved.getName());
            saved.setId("new-id");
            return saved;
        });
        var response = controller.register(new UsuarioRequestDTO(" test@example.com ", "password", "Test User"));
        assertEquals(200, response.getStatusCode().value());
        assertEquals("test@example.com", response.getBody().email());
    }
    @Test void weakPasswordIsRejectedBeforeWritingUser() {
        assertEquals(400, controller.register(new UsuarioRequestDTO("test@example.com", "x", "Test")).getStatusCode().value());
        verifyNoInteractions(users);
    }
    @Test void googleWithoutServerConfigurationReturnsServiceUnavailable() {
        try (var firebase = mockStatic(FirebaseAuth.class)) {
            firebase.when(FirebaseAuth::getInstance).thenThrow(new IllegalStateException("not configured"));
            assertEquals(503, controller.loginWithGoogle(Map.of("idToken", "test-token")).getStatusCode().value());
        }
    }
}
