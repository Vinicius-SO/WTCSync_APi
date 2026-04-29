package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.dtos.AuthRequestDTO;
import com.fiap.WtcSync.application.dtos.AuthResponseDTO;
import com.fiap.WtcSync.application.dtos.UsuarioRequestDTO;
import com.fiap.WtcSync.application.dtos.UsuarioResponseDTO;
import com.fiap.WtcSync.application.services.TokenService;
import com.fiap.WtcSync.domain.entities.User;
import com.fiap.WtcSync.domain.interfaces.IUserRepository;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final IUserRepository userRepository;

    public AuthController(TokenService tokenService, PasswordEncoder passwordEncoder, IUserRepository userRepository) {
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPassword())) {
            return ResponseEntity.status(401).build();
        }

        User user = userOpt.get();
        if (!user.getActive()) {
            return ResponseEntity.status(401).build();
        }

        String token = tokenService.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponseDTO(token, user.getEmail(), tokenService.getExpiration()));
    }

    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Create a new user account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Email already exists")
    })
    public ResponseEntity<UsuarioResponseDTO> register(@RequestBody UsuarioRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.badRequest().build();
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User("New User", request.email(), encodedPassword, "CLIENT");
        User saved = userRepository.save(user);

        return ResponseEntity.ok(new UsuarioResponseDTO(saved.getId(), saved.getEmail()));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get authenticated user info")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User info retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, String>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return ResponseEntity.ok(Map.of("email", email));
    }
}
