package com.luxrobo.demoapi.service;

import com.luxrobo.demoapi.entity.User;
import com.luxrobo.demoapi.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    public Map<String, Object> getProfile(Long userId) {
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

    public Map<String, Object> updateInterests(Long userId, List<String> interests) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setInterests(String.join(",", interests));
        userRepository.save(user);

        return Map.of("interests", user.getInterests());
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}
