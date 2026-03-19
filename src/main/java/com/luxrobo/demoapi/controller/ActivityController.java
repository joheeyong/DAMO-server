package com.luxrobo.demoapi.controller;

import com.luxrobo.demoapi.service.ActivityService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping("/search")
    public Map<String, Object> recordSearch(@RequestBody Map<String, String> body, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Map.of("status", "skipped");
        }
        Long userId = (Long) authentication.getPrincipal();
        return activityService.recordSearch(userId, body.get("query"));
    }

    @PostMapping("/click")
    public Map<String, Object> recordClick(@RequestBody Map<String, String> body, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Map.of("status", "skipped");
        }
        Long userId = (Long) authentication.getPrincipal();
        return activityService.recordClick(userId, body.get("contentId"), body.get("platform"), body.getOrDefault("sourceKeyword", ""));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/rank")
    public Map<String, Object> rankItems(@RequestBody Map<String, Object> body, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Map.of("rankedIds", List.of());
        }
        Long userId = (Long) authentication.getPrincipal();
        List<Map<String, String>> items = (List<Map<String, String>>) body.get("items");
        return activityService.rankItems(userId, items);
    }
}
