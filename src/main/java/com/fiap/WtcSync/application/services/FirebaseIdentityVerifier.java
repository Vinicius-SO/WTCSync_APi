package com.fiap.WtcSync.application.services;

import com.google.auth.oauth2.TokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.util.Map;

/** Verifies Firebase client ID tokens using Google's public signing certificates.
 * Administrative service-account credentials are only needed for privileged Firebase operations.
 */
@Service
public class FirebaseIdentityVerifier {
    public static final String CERTIFICATES_URL =
        "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";
    private final TokenVerifier verifier;
    private final Clock clock;

    @Autowired
    public FirebaseIdentityVerifier(@Value("${FIREBASE_PROJECT_ID:wtcsync}") String projectId) {
        this(TokenVerifier.newBuilder()
            .setAudience(projectId)
            .setIssuer("https://securetoken.google.com/" + projectId)
            .setCertificatesLocation(CERTIFICATES_URL)
            .build(), Clock.systemUTC());
    }

    FirebaseIdentityVerifier(TokenVerifier verifier, Clock clock) {
        this.verifier = verifier;
        this.clock = clock;
    }

    public String verifiedGoogleEmail(String token) throws TokenVerifier.VerificationException {
        if (token == null || token.isBlank() || token.length() > 16384)
            throw new TokenVerifier.VerificationException("Invalid ID token");
        try {
            var jwt = verifier.verify(token);
            var payload = jwt.getPayload();
            long now = clock.instant().getEpochSecond();
            Object authTime = payload.get("auth_time");
            Object firebase = payload.get("firebase");
            Object email = payload.get("email");
            if (!"RS256".equals(jwt.getHeader().getAlgorithm())
                || jwt.getHeader().getKeyId() == null || jwt.getHeader().getKeyId().isBlank()
                || payload.getSubject() == null || payload.getSubject().isBlank() || payload.getSubject().length() > 128
                || payload.getExpirationTimeSeconds() == null || payload.getExpirationTimeSeconds() <= now
                || payload.getIssuedAtTimeSeconds() == null || payload.getIssuedAtTimeSeconds() > now
                || !(authTime instanceof Number) || ((Number) authTime).longValue() > now
                || ((Number) authTime).longValue() <= 0
                || !Boolean.TRUE.equals(payload.get("email_verified"))
                || !(email instanceof String) || ((String) email).isBlank()
                || !(firebase instanceof Map) || !"google.com".equals(((Map<?, ?>) firebase).get("sign_in_provider"))) {
                throw new TokenVerifier.VerificationException("Invalid Google Firebase identity");
            }
            return (String) email;
        } catch (IllegalArgumentException e) {
            throw new TokenVerifier.VerificationException("Malformed ID token", e);
        }
    }
}
