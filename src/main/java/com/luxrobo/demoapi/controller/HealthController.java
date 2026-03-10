package com.luxrobo.demoapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP/DOWN",
                "timestamp", LocalDateTime.now().toString()
        );
    }

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of("message", "Welcome to Demo API");
    }
}
