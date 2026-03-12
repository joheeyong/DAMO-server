package com.luxrobo.demoapi.service;

import com.luxrobo.demoapi.entity.User;
import com.luxrobo.demoapi.repository.UserRepository;
import com.luxrobo.demoapi.security.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class OAuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final WebClient webClient;

    @Value("${google.client-id}")
    private String googleClientId;

    @Value("${google.client-secret}")
    private String googleClientSecret;

    @Value("${naver.login.client-id}")
    private String naverClientId;

    @Value("${naver.login.client-secret}")
    private String naverClientSecret;

    public OAuthService(UserRepository userRepository, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.webClient = WebClient.create();
    }

    public Map<String, Object> loginWithGoogle(String code, String redirectUri) {
        // 1. Exchange code for access token
        Map tokenResponse = webClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .bodyValue(Map.of(
                        "code", code,
                        "client_id", googleClientId,
                        "client_secret", googleClientSecret,
                        "redirect_uri", redirectUri,
                        "grant_type", "authorization_code"
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String accessToken = (String) tokenResponse.get("access_token");

        // 2. Get user info
        Map userInfo = webClient.get()
                .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String providerId = String.valueOf(userInfo.get("id"));
        String name = (String) userInfo.get("name");
        String email = (String) userInfo.get("email");
        String picture = (String) userInfo.get("picture");

        // 3. Upsert user
        User user = upsertUser("google", providerId, name, email, picture);

        // 4. Generate JWT
        String token = jwtProvider.generateToken(user.getId());

        return Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.getId(),
                        "name", user.getName() != null ? user.getName() : "",
                        "email", user.getEmail() != null ? user.getEmail() : "",
                        "profileImage", user.getProfileImage() != null ? user.getProfileImage() : "",
                        "provider", user.getProvider()
                )
        );
    }

    public Map<String, Object> loginWithNaver(String code, String state, String redirectUri) {
        // 1. Exchange code for access token
        Map tokenResponse = webClient.post()
                .uri("https://nid.naver.com/oauth2.0/token"
                        + "?grant_type=authorization_code"
                        + "&client_id=" + naverClientId
                        + "&client_secret=" + naverClientSecret
                        + "&code=" + code
                        + "&state=" + state)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String accessToken = (String) tokenResponse.get("access_token");

        // 2. Get user info
        Map userInfoResponse = webClient.get()
                .uri("https://openapi.naver.com/v1/nid/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map response = (Map) userInfoResponse.get("response");
        String providerId = (String) response.get("id");
        String name = (String) response.get("name");
        String email = (String) response.get("email");
        String picture = (String) response.get("profile_image");

        // 3. Upsert user
        User user = upsertUser("naver", providerId, name, email, picture);

        // 4. Generate JWT
        String token = jwtProvider.generateToken(user.getId());

        return Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.getId(),
                        "name", user.getName() != null ? user.getName() : "",
                        "email", user.getEmail() != null ? user.getEmail() : "",
                        "profileImage", user.getProfileImage() != null ? user.getProfileImage() : "",
                        "provider", user.getProvider()
                )
        );
    }

    private User upsertUser(String provider, String providerId, String name, String email, String profileImage) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .map(existing -> {
                    existing.setName(name);
                    existing.setEmail(email);
                    existing.setProfileImage(profileImage);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    User newUser = new User(name, email, provider, providerId, profileImage);
                    return userRepository.save(newUser);
                });
    }
}
