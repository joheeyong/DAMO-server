package com.luxrobo.demoapi.service;

import com.luxrobo.demoapi.entity.DeviceToken;
import com.luxrobo.demoapi.entity.User;
import com.luxrobo.demoapi.repository.DeviceTokenRepository;
import com.luxrobo.demoapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);
    private static final int MIN_INTERESTS = 3;

    private final UserRepository userRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmService fcmService;

    public NotificationScheduler(UserRepository userRepository,
                                  DeviceTokenRepository deviceTokenRepository,
                                  FcmService fcmService) {
        this.userRepository = userRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.fcmService = fcmService;
    }

    @PostConstruct
    public void scheduleTestPush() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime target = now.withHour(15).withMinute(20).withSecond(0);
        long delaySeconds = java.time.Duration.between(now, target).getSeconds();
        if (delaySeconds > 0) {
            log.info("Test push scheduled in {} seconds (KST 15:20)", delaySeconds);
            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                log.info("Firing one-time test push...");
                sendInterestReminder();
            }, delaySeconds, TimeUnit.SECONDS);
        }
    }

    @Scheduled(fixedRate = 3600000) // 1 hour
    public void sendInterestReminder() {
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));
        if (now.isBefore(LocalTime.of(8, 0))) {
            log.info("Skipping interest reminder — quiet hours (KST {}).", now);
            return;
        }
        log.info("Running interest reminder notification job (KST {})...", now);

        List<User> allUsers = userRepository.findAll();
        int sentCount = 0;

        for (User user : allUsers) {
            int interestCount = countInterests(user.getInterests());
            if (interestCount >= MIN_INTERESTS) continue;

            List<DeviceToken> tokens = deviceTokenRepository.findByUserId(user.getId());
            if (tokens.isEmpty()) continue;

            String name = user.getName() != null ? user.getName() : "회원";
            String title = "DAMO";
            String body = name + "님! 관심사가 " + interestCount + "개뿐이에요! 관심사를 추가해보세요!";

            Map<String, String> data = Map.of("action", "open_interests");

            List<String> tokenStrings = tokens.stream()
                    .map(DeviceToken::getToken)
                    .toList();

            try {
                fcmService.sendToAll(tokenStrings, title, body, data);
                sentCount++;
            } catch (Exception e) {
                log.error("Failed to send interest reminder to user {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("Interest reminder job complete. Sent to {} users.", sentCount);
    }

    private int countInterests(String interests) {
        if (interests == null || interests.trim().isEmpty()) return 0;
        return (int) java.util.Arrays.stream(interests.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .count();
    }
}
