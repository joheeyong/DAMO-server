package com.luxrobo.demoapi.controller;

import com.luxrobo.demoapi.entity.User;
import com.luxrobo.demoapi.repository.UserRepository;
import com.luxrobo.demoapi.service.OAuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OAuthService oAuthService;
    private final UserRepository userRepository;

    public AuthController(OAuthService oAuthService, UserRepository userRepository) {
        this.oAuthService = oAuthService;
        this.userRepository = userRepository;
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
    public Map<String, Object> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Map.of("error", "Unauthorized", "status", 403);
        }
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String interests = user.getInterests() != null ? user.getInterests() : "";
        return Map.of(
                "id", user.getId(),
                "name", user.getName() != null ? user.getName() : "",
                "email", user.getEmail() != null ? user.getEmail() : "",
                "profileImage", user.getProfileImage() != null ? user.getProfileImage() : "",
                "provider", user.getProvider() != null ? user.getProvider() : "",
                "interests", interests
        );
    }

    @PutMapping("/interests")
    public Map<String, Object> updateInterests(@RequestBody Map<String, Object> body, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Map.of("error", "Unauthorized", "status", 403);
        }
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> interests = (List<String>) body.get("interests");
        user.setInterests(String.join(",", interests));
        userRepository.save(user);

        return Map.of("interests", user.getInterests());
    }
}
