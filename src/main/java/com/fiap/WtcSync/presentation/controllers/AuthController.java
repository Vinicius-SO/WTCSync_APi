package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.dtos.AuthRequestDTO;
import com.fiap.WtcSync.application.dtos.AuthResponseDTO;
import com.fiap.WtcSync.application.dtos.FirebaseTokenResponseDTO;
import com.fiap.WtcSync.application.dtos.UsuarioRequestDTO;
import com.fiap.WtcSync.application.dtos.UsuarioResponseDTO;
import com.fiap.WtcSync.application.services.TokenService;
import com.fiap.WtcSync.domain.entities.User;
import com.fiap.WtcSync.domain.interfaces.IUserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.auth.oauth2.TokenVerifier;
import com.fiap.WtcSync.application.services.FirebaseIdentityVerifier;
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
  private final FirebaseIdentityVerifier firebaseIdentityVerifier;

  public AuthController(TokenService tokenService, PasswordEncoder passwordEncoder, IUserRepository userRepository, FirebaseIdentityVerifier firebaseIdentityVerifier) {
    this.tokenService = tokenService;
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
    this.firebaseIdentityVerifier = firebaseIdentityVerifier;
  }

  @PostMapping("/login")
  @Operation(summary = "User login", description = "Authenticate user and return JWT token")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Login successful"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials")
  })
  public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {
    if (request == null || request.email() == null || request.email().isBlank()
        || request.password() == null || request.password().isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    Optional<User> userOpt = userRepository.findByEmail(request.email().trim());

    if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPassword())) {
      return ResponseEntity.status(401).build();
    }

    User user = userOpt.get();
    if (!Boolean.TRUE.equals(user.getActive())) {
      return ResponseEntity.status(401).build();
    }

    String token = tokenService.generateToken(user.getEmail());
    return ResponseEntity.ok(sessionResponse(token, user));
  }

  @PostMapping("/google")
  @Operation(summary = "Exchange Google Firebase ID token verified with public signing certificates for API JWT")
  public ResponseEntity<?> loginWithGoogle(@RequestBody Map<String, String> request) {
    String idToken = request.get("idToken");
    if (idToken == null || idToken.isBlank()) return ResponseEntity.badRequest().build();
    try {
      String email = firebaseIdentityVerifier.verifiedGoogleEmail(idToken);
      Optional<User> existing = userRepository.findByEmail(email);
      if (existing.isEmpty() || !Boolean.TRUE.equals(existing.get().getActive())) {
        return ResponseEntity.status(403).build();
      }
      User user = existing.get();
      String token = tokenService.generateToken(user.getEmail());
      return ResponseEntity.ok(sessionResponse(token, user));
    } catch (TokenVerifier.VerificationException e) {
      return ResponseEntity.status(401).build();
    } catch (IllegalStateException e) {
      return ResponseEntity.status(503).build();
    }
  }

  private AuthResponseDTO sessionResponse(String token, User user) {
    var profile = new AuthResponseDTO.Profile(user.getId() == null ? user.getEmail() : user.getId(),
        user.getEmail(), user.getName() == null ? user.getEmail() : user.getName(),
        com.fiap.WtcSync.application.services.AccountAccess.canonicalRole(user.getRole()));
    return new AuthResponseDTO(token, user.getEmail(), tokenService.getExpiration(), profile);
  }

  @PostMapping("/register")
  @Operation(summary = "Register user", description = "Create a new user account")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User registered successfully"),
      @ApiResponse(responseCode = "400", description = "Email already exists")
  })
  public ResponseEntity<UsuarioResponseDTO> register(@RequestBody UsuarioRequestDTO request) {
    if (request == null || request.email() == null
        || !request.email().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        || request.password() == null || request.password().length() < 6) {
      return ResponseEntity.badRequest().build();
    }
    String email = request.email().trim();
    if (userRepository.existsByEmail(email)) {
      return ResponseEntity.badRequest().build();
    }

    String encodedPassword = passwordEncoder.encode(request.password());
    String name = request.name() == null || request.name().isBlank() ? "New User" : request.name().trim();
    User user = new User(name, email, encodedPassword, "CLIENT");
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
    if (auth == null || !auth.isAuthenticated()) return ResponseEntity.status(401).build();
    String email = auth.getName();
    return userRepository.findByEmail(email)
        .filter(user -> Boolean.TRUE.equals(user.getActive()))
        .map(user -> ResponseEntity.ok(Map.of(
            "email", user.getEmail(), "role", user.getRole(),
            "id", user.getId() == null ? user.getEmail() : user.getId(),
            "name", user.getName() == null ? user.getEmail() : user.getName())))
        .orElseGet(() -> ResponseEntity.status(401).build());
  }

  @PostMapping("/firebase-token")
  @Operation(summary = "Generate Firebase Custom Token", description = "Generate a Firebase Custom Token for the authenticated user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Token generated successfully"),
      @ApiResponse(responseCode = "500", description = "Failed to generate Firebase token")
  })
  public ResponseEntity<FirebaseTokenResponseDTO> generateFirebaseToken() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String uid = auth.getName();
    try {
      String customToken = FirebaseAuth.getInstance().createCustomTokenAsync(uid).get();
      return ResponseEntity.ok(new FirebaseTokenResponseDTO(customToken));
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }
}
