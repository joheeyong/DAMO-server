package com.luxrobo.demoapi.controller;

import com.luxrobo.demoapi.entity.DeviceToken;
import com.luxrobo.demoapi.repository.DeviceTokenRepository;
import com.luxrobo.demoapi.service.FcmService;
import org.springframework.security.core.Authentication;
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
    public Map<String, String> registerToken(@RequestBody Map<String, String> request, Authentication authentication) {
        String token = request.get("token");
        String platform = request.getOrDefault("platform", "unknown");

        if (token == null || token.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Token is required");
        }

        Long userId = null;
        if (authentication != null && authentication.getPrincipal() != null) {
            userId = (Long) authentication.getPrincipal();
        }

        var existing = deviceTokenRepository.findByToken(token);
        if (existing.isPresent()) {
            // Update userId if user is now logged in
            DeviceToken dt = existing.get();
            if (userId != null && !userId.equals(dt.getUserId())) {
                dt.setUserId(userId);
                deviceTokenRepository.save(dt);
            }
        } else {
            DeviceToken dt = new DeviceToken(token, platform);
            dt.setUserId(userId);
            deviceTokenRepository.save(dt);
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
        String targetUserId = request.get("userId");

        try {
            String result;
            if (targetToken != null && !targetToken.isEmpty()) {
                // Send to specific token
                result = fcmService.sendToToken(targetToken, title, body);
            } else if (targetUserId != null && !targetUserId.isEmpty()) {
                // Send to specific user's all devices
                Long uid = Long.parseLong(targetUserId);
                List<String> userTokens = deviceTokenRepository.findByUserId(uid)
                        .stream().map(DeviceToken::getToken).toList();
                if (userTokens.isEmpty()) {
                    return Map.of("status", "OK", "result", "No devices for user");
                }
                result = fcmService.sendToAll(userTokens, title, body);
            } else {
                // Send to all devices
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
