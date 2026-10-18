package com.fiap.WtcSync.application.services;

import com.google.auth.oauth2.TokenVerifier;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FirebaseIdentityVerifierTest {
    static final long NOW = 1802880000L;
    KeyPair keys;
    FirebaseIdentityVerifier verifier;
    Map<String, Object> claims;

    @BeforeEach void setup() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        keys = generator.generateKeyPair();
        verifier = new FirebaseIdentityVerifier(TokenVerifier.newBuilder()
            .setIssuer("https://securetoken.google.com/wtcsync").setAudience("wtcsync")
            .setPublicKey(keys.getPublic()).setClock(() -> NOW * 1000).build(),
            Clock.fixed(Instant.ofEpochSecond(NOW), ZoneOffset.UTC));
        claims = new HashMap<>(Map.of("iss", "https://securetoken.google.com/wtcsync",
            "aud", "wtcsync", "sub", "firebase-user", "iat", NOW - 10, "exp", NOW + 3600,
            "auth_time", NOW - 60, "email", "user@example.com", "email_verified", true,
            "firebase", Map.of("sign_in_provider", "google.com")));
    }
    String token() {
        return Jwts.builder().header().keyId("test-key").and().claims(claims)
            .signWith(keys.getPrivate(), Jwts.SIG.RS256).compact();
    }
    void rejected() { assertThrows(TokenVerifier.VerificationException.class, () -> verifier.verifiedGoogleEmail(token())); }
    @Test void acceptsValidGoogleIdentityWithoutAdminCredentials() throws Exception {
        assertEquals("user@example.com", verifier.verifiedGoogleEmail(token()));
    }
    @Test void rejectsOtherProject() { claims.put("aud", "other-project"); rejected(); }
    @Test void rejectsOtherIssuer() { claims.put("iss", "https://attacker.example"); rejected(); }
    @Test void rejectsExpiredToken() { claims.put("exp", NOW - 1); rejected(); }
    @Test void rejectsMissingExpiration() { claims.remove("exp"); rejected(); }
    @Test void rejectsFutureIssuedTime() { claims.put("iat", NOW + 60); rejected(); }
    @Test void rejectsFutureAuthenticationTime() { claims.put("auth_time", NOW + 60); rejected(); }
    @Test void rejectsMissingAuthenticationTime() { claims.remove("auth_time"); rejected(); }
    @Test void rejectsEmptySubject() { claims.put("sub", ""); rejected(); }
    @Test void rejectsUnverifiedEmail() { claims.put("email_verified", false); rejected(); }
    @Test void rejectsPasswordProviderOnGoogleRoute() { claims.put("firebase", Map.of("sign_in_provider", "password")); rejected(); }
    @Test void rejectsWrongSigningKey() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        keys = generator.generateKeyPair(); rejected();
    }
    @Test void rejectsMalformedToken() {
        assertThrows(TokenVerifier.VerificationException.class, () -> verifier.verifiedGoogleEmail("invalid-test-token"));
    }
}
