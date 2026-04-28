package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.dtos.AuthRequestDTO;
import com.fiap.WtcSync.application.dtos.AuthResponseDTO;
import com.fiap.WtcSync.application.services.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(TokenService tokenService, PasswordEncoder passwordEncoder) {
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {
        String username = request.username();
        String password = request.password();

        if (!validateCredentials(username, password)) {
            return ResponseEntity.status(401).build();
        }

        String token = tokenService.generateToken(username);
        return ResponseEntity.ok(new AuthResponseDTO(token, username, tokenService.getExpiration()));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get authenticated user info")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User info retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, String>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return ResponseEntity.ok(Map.of("username", username));
    }

    private boolean validateCredentials(String username, String password) {
        return "admin".equals(username) && "admin123".equals(password);
    }
}