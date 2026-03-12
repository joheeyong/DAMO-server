package com.luxrobo.demoapi.service;

import com.luxrobo.demoapi.entity.User;
import com.luxrobo.demoapi.entity.UserClickHistory;
import com.luxrobo.demoapi.entity.UserSearchHistory;
import com.luxrobo.demoapi.repository.UserClickHistoryRepository;
import com.luxrobo.demoapi.repository.UserRepository;
import com.luxrobo.demoapi.repository.UserSearchHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final UserRepository userRepository;
    private final UserSearchHistoryRepository searchHistoryRepository;
    private final UserClickHistoryRepository clickHistoryRepository;

    public RecommendationService(UserRepository userRepository,
                                  UserSearchHistoryRepository searchHistoryRepository,
                                  UserClickHistoryRepository clickHistoryRepository) {
        this.userRepository = userRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.clickHistoryRepository = clickHistoryRepository;
    }

    /**
     * Rank content items based on user profile, search history, and click history.
     * Returns ordered list of content IDs (highest score first).
     */
    public List<String> rankItems(Long userId, List<Map<String, String>> items) {
        User user = userRepository.findById(userId).orElse(null);
        List<UserSearchHistory> recentSearches = searchHistoryRepository.findTop50ByUserIdOrderBySearchedAtDesc(userId);
        List<UserClickHistory> recentClicks = clickHistoryRepository.findTop100ByUserIdOrderByClickedAtDesc(userId);

        // Build user profile
        List<String> interests = new ArrayList<>();
        if (user != null && user.getInterests() != null && !user.getInterests().isEmpty()) {
            interests = Arrays.stream(user.getInterests().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        Set<String> clickedContentIds = recentClicks.stream()
                .map(UserClickHistory::getContentId)
                .collect(Collectors.toSet());

        // Platform click counts with decay
        Map<String, Double> platformAffinity = new HashMap<>();
        for (UserClickHistory click : recentClicks) {
            double d = decay(click.getClickedAt());
            platformAffinity.merge(click.getPlatform(), d, Double::sum);
        }

        // Keyword affinity from clicks
        Map<String, Double> keywordAffinity = new HashMap<>();
        for (UserClickHistory click : recentClicks) {
            if (click.getSourceKeyword() != null && !click.getSourceKeyword().isEmpty()) {
                double d = decay(click.getClickedAt());
                keywordAffinity.merge(click.getSourceKeyword(), d, Double::sum);
            }
        }

        // Search keyword affinity
        Map<String, Double> searchAffinity = new HashMap<>();
        for (UserSearchHistory search : recentSearches) {
            double d = decay(search.getSearchedAt());
            searchAffinity.merge(search.getQuery().toLowerCase(), d, Double::sum);
        }

        LocalDateTime now = LocalDateTime.now();
        Random random = new Random();

        // Score each item
        List<ScoredItem> scored = new ArrayList<>();
        for (Map<String, String> item : items) {
            String id = item.getOrDefault("id", "");
            String platform = item.getOrDefault("platform", "");
            String title = item.getOrDefault("title", "").toLowerCase();
            String sourceKeyword = item.getOrDefault("sourceKeyword", "");

            double score = 0.0;

            // 1. Interest match (+10 per match)
            for (String interest : interests) {
                String lowerInterest = interest.toLowerCase();
                if (sourceKeyword.toLowerCase().contains(lowerInterest) || title.contains(lowerInterest)) {
                    score += 10.0;
                }
            }

            // 2. Search history match (+5 * decay)
            for (Map.Entry<String, Double> entry : searchAffinity.entrySet()) {
                String searchQuery = entry.getKey();
                if (!sourceKeyword.isEmpty() && sourceKeyword.toLowerCase().contains(searchQuery)) {
                    score += 5.0 * Math.min(entry.getValue(), 3.0);
                } else if (title.contains(searchQuery)) {
                    score += 3.0 * Math.min(entry.getValue(), 3.0);
                }
            }

            // 3. Platform affinity (+0.3 * decayed count, capped)
            Double platformScore = platformAffinity.get(platform);
            if (platformScore != null) {
                score += Math.min(platformScore, 10.0) * 0.3;
            }

            // 4. Keyword affinity from clicks (+7 * decay)
            if (!sourceKeyword.isEmpty()) {
                Double kwScore = keywordAffinity.get(sourceKeyword);
                if (kwScore != null) {
                    score += 7.0 * Math.min(kwScore, 3.0);
                }
            }

            // 5. Already-clicked penalty
            if (clickedContentIds.contains(id)) {
                score -= 15.0;
            }

            // 6. Small random jitter for variety
            score += random.nextDouble() * 0.5;

            scored.add(new ScoredItem(id, score));
        }

        // Sort by score descending
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        return scored.stream().map(s -> s.id).collect(Collectors.toList());
    }

    /**
     * Time decay: recent actions matter more.
     * Half-life of 72 hours.
     */
    private double decay(LocalDateTime timestamp) {
        if (timestamp == null) return 0.5;
        long hoursAgo = Duration.between(timestamp, LocalDateTime.now()).toHours();
        return 1.0 / (1.0 + hoursAgo / 72.0);
    }

    private static class ScoredItem {
        final String id;
        final double score;

        ScoredItem(String id, double score) {
            this.id = id;
            this.score = score;
        }
    }
}
