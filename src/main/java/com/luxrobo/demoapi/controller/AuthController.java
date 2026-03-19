package com.luxrobo.demoapi.controller;

import com.luxrobo.demoapi.service.OAuthService;
import com.luxrobo.demoapi.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OAuthService oAuthService;
    private final UserService userService;

    public AuthController(OAuthService oAuthService, UserService userService) {
        this.oAuthService = oAuthService;
        this.userService = userService;
    }

    @PostMapping("/google")
    public Map<String, Object> googleLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String redirectUri = body.get("redirectUri");
        return oAuthService.loginWithGoogle(code, redirectUri);
    }

    @PostMapping("/naver")
    public Map<String, Object> naverLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String state = body.get("state");
        String redirectUri = body.get("redirectUri");
        return oAuthService.loginWithNaver(code, state, redirectUri);
    }

    @PostMapping("/kakao")
    public Map<String, Object> kakaoLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String redirectUri = body.get("redirectUri");
        return oAuthService.loginWithKakao(code, redirectUri);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @SuppressWarnings("unchecked")
    @PutMapping("/interests")
    public ResponseEntity<?> updateInterests(@RequestBody Map<String, Object> body, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        Long userId = (Long) authentication.getPrincipal();
        List<String> interests = (List<String>) body.get("interests");
        return ResponseEntity.ok(userService.updateInterests(userId, interests));
    }
}
