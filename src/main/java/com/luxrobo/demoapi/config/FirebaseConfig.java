package com.luxrobo.demoapi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

@Component
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                String credPath = System.getenv("FIREBASE_CREDENTIALS");
                GoogleCredentials credentials;

                if (credPath != null && !credPath.isEmpty()) {
                    // 경로 정규화하여 path traversal 방지
                    Path normalizedPath = Path.of(credPath).normalize().toAbsolutePath();
                    credentials = GoogleCredentials.fromStream(new FileInputStream(normalizedPath.toFile()));
                } else {
                    credentials = GoogleCredentials.getApplicationDefault();
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            }
        } catch (IOException e) {
            log.error("Firebase initialization failed");
        }
    }
}
