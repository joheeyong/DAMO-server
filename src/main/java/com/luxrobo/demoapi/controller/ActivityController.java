package com.luxrobo.demoapi.controller;

import com.luxrobo.demoapi.entity.UserClickHistory;
import com.luxrobo.demoapi.entity.UserSearchHistory;
import com.luxrobo.demoapi.repository.UserClickHistoryRepository;
import com.luxrobo.demoapi.repository.UserSearchHistoryRepository;
import com.luxrobo.demoapi.service.RecommendationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final UserSearchHistoryRepository searchHistoryRepository;
    private final UserClickHistoryRepository clickHistoryRepository;
    private final RecommendationService recommendationService;

    public ActivityController(UserSearchHistoryRepository searchHistoryRepository,
                               UserClickHistoryRepository clickHistoryRepository,
                               RecommendationService recommendationService) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.clickHistoryRepository = clickHistoryRepository;
        this.recommendationService = recommendationService;
    }

    @PostMapping("/search")
    public Map<String, Object> recordSearch(@RequestBody Map<String, String> body, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Map.of("status", "skipped");
        }
        Long userId = (Long) authentication.getPrincipal();
        String query = body.get("query");
        if (query == null || query.trim().isEmpty()) {
            return Map.of("status", "skipped");
        }

        UserSearchHistory history = new UserSearchHistory();
        history.setUserId(userId);
        history.setQuery(query.trim());
        searchHistoryRepository.save(history);

        return Map.of("status", "ok");
    }

    @PostMapping("/click")
    public Map<String, Object> recordClick(@RequestBody Map<String, String> body, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Map.of("status", "skipped");
        }
        Long userId = (Long) authentication.getPrincipal();
        String contentId = body.get("contentId");
        String platform = body.get("platform");
        if (contentId == null || platform == null) {
            return Map.of("status", "skipped");
        }

        UserClickHistory history = new UserClickHistory();
        history.setUserId(userId);
        history.setContentId(contentId);
        history.setPlatform(platform);
        history.setSourceKeyword(body.getOrDefault("sourceKeyword", ""));
        clickHistoryRepository.save(history);

        return Map.of("status", "ok");
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/rank")
    public Map<String, Object> rankItems(@RequestBody Map<String, Object> body, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Map.of("rankedIds", List.of());
        }
        Long userId = (Long) authentication.getPrincipal();
        List<Map<String, String>> items = (List<Map<String, String>>) body.get("items");
        if (items == null || items.isEmpty()) {
            return Map.of("rankedIds", List.of());
        }

        List<String> rankedIds = recommendationService.rankItems(userId, items);
        return Map.of("rankedIds", rankedIds);
    }
}
