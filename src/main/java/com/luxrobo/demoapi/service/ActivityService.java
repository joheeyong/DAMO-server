package com.luxrobo.demoapi.service;

import com.luxrobo.demoapi.entity.UserClickHistory;
import com.luxrobo.demoapi.entity.UserSearchHistory;
import com.luxrobo.demoapi.repository.UserClickHistoryRepository;
import com.luxrobo.demoapi.repository.UserSearchHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ActivityService {

    private final UserSearchHistoryRepository searchHistoryRepository;
    private final UserClickHistoryRepository clickHistoryRepository;
    private final RecommendationService recommendationService;

    public ActivityService(UserSearchHistoryRepository searchHistoryRepository,
                           UserClickHistoryRepository clickHistoryRepository,
                           RecommendationService recommendationService) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.clickHistoryRepository = clickHistoryRepository;
        this.recommendationService = recommendationService;
    }

    public Map<String, Object> recordSearch(Long userId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return Map.of("status", "skipped");
        }

        UserSearchHistory history = new UserSearchHistory();
        history.setUserId(userId);
        history.setQuery(query.trim());
        searchHistoryRepository.save(history);

        return Map.of("status", "ok");
    }

    public Map<String, Object> recordClick(Long userId, String contentId, String platform, String sourceKeyword) {
        if (contentId == null || platform == null) {
            return Map.of("status", "skipped");
        }

        UserClickHistory history = new UserClickHistory();
        history.setUserId(userId);
        history.setContentId(contentId);
        history.setPlatform(platform);
        history.setSourceKeyword(sourceKeyword != null ? sourceKeyword : "");
        clickHistoryRepository.save(history);

        return Map.of("status", "ok");
    }

    public Map<String, Object> rankItems(Long userId, List<Map<String, String>> items) {
        if (items == null || items.isEmpty()) {
            return Map.of("rankedIds", List.of());
        }

        List<String> rankedIds = recommendationService.rankItems(userId, items);
        return Map.of("rankedIds", rankedIds);
    }
}
