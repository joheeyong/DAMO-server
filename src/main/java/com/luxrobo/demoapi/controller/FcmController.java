package com.luxrobo.demoapi.controller;

import com.luxrobo.demoapi.entity.DeviceToken;
import com.luxrobo.demoapi.repository.DeviceTokenRepository;
import com.luxrobo.demoapi.service.FcmService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fcm")
public class FcmController {

    private final FcmService fcmService;
    private final DeviceTokenRepository deviceTokenRepository;

    public FcmController(FcmService fcmService, DeviceTokenRepository deviceTokenRepository) {
        this.fcmService = fcmService;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @PostMapping("/register")
    public Map<String, String> registerToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String platform = request.getOrDefault("platform", "unknown");

        if (token == null || token.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Token is required");
        }

        if (!deviceTokenRepository.existsByToken(token)) {
            deviceTokenRepository.save(new DeviceToken(token, platform));
        }

        return Map.of("status", "OK", "message", "Token registered");
    }

    @GetMapping("/tokens")
    public List<DeviceToken> getTokens() {
        return deviceTokenRepository.findAll();
    }

    @PostMapping("/send")
    public Map<String, String> sendNotification(@RequestBody Map<String, String> request) {
        String title = request.getOrDefault("title", "DAMO");
        String body = request.getOrDefault("body", "");
        String targetToken = request.get("token");

        try {
            String result;
            if (targetToken != null && !targetToken.isEmpty()) {
                result = fcmService.sendToToken(targetToken, title, body);
            } else {
                List<String> allTokens = deviceTokenRepository.findAll()
                        .stream().map(DeviceToken::getToken).toList();
                result = fcmService.sendToAll(allTokens, title, body);
            }
            return Map.of("status", "OK", "result", result);
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }
}
