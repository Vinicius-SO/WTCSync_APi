package com.fiap.WtcSync.infrastructure.configs;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @PostConstruct
    public void initialize() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                String credentialsPath = System.getenv("FIREBASE_CREDENTIALS_PATH");
                InputStream serviceAccount = credentialsPath != null
                    ? new FileInputStream(credentialsPath)
                    : getClass().getResourceAsStream("/firebase-service-account.json");

                if (serviceAccount == null) {
                    log.warn("Firebase credentials not found. Push notifications will be disabled.");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            } catch (Exception e) {
                log.warn("Failed to initialize Firebase: {}. Push notifications will be disabled.", e.getMessage());
            }
        }
    }
}
