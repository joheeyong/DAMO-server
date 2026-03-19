package com.luxrobo.demoapi.controller;

import com.luxrobo.demoapi.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final NaverSearchService naverSearchService;
    private final YouTubeSearchService youTubeSearchService;
    private final InstagramSearchService instagramSearchService;
    private final KakaoSearchService kakaoSearchService;
    private final SuggestionService suggestionService;
    private final SearchOrchestrationService searchOrchestrationService;

    public SearchController(NaverSearchService naverSearchService,
                            YouTubeSearchService youTubeSearchService,
                            InstagramSearchService instagramSearchService,
                            KakaoSearchService kakaoSearchService,
                            SuggestionService suggestionService,
                            SearchOrchestrationService searchOrchestrationService) {
        this.naverSearchService = naverSearchService;
        this.youTubeSearchService = youTubeSearchService;
        this.instagramSearchService = instagramSearchService;
        this.kakaoSearchService = kakaoSearchService;
        this.suggestionService = suggestionService;
        this.searchOrchestrationService = searchOrchestrationService;
    }

    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam String q) {
        return suggestionService.getSuggestions(q);
    }

    @GetMapping("/{category}")
    public String searchByCategory(
            @PathVariable String category,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int display,
            @RequestParam(defaultValue = "1") int start,
            @RequestParam(defaultValue = "sim") String sort
    ) throws Exception {
        display = Math.max(1, Math.min(display, 50));
        start = Math.max(1, Math.min(start, 1000));
        if (query.length() > 200) {
            query = query.substring(0, 200);
        }
        if ("youtube".equals(category)) {
            return youTubeSearchService.search(query, display, sort);
        }
        if ("shorts".equals(category)) {
            return youTubeSearchService.searchShorts(query, display, sort);
        }
        if ("instagram".equals(category)) {
            return instagramSearchService.searchByHashtag(query, display);
        }
        if (category.startsWith("kakao-")) {
            return kakaoSearchService.search(category, query, display, 1, "accuracy");
        }
        return naverSearchService.search(category, query, display, start, sort);
    }

    @GetMapping("/all")
    public Map<String, Object> searchAll(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int display,
            @RequestParam(defaultValue = "sim") String sort,
            @RequestParam(defaultValue = "all") String period
    ) {
        display = Math.max(1, Math.min(display, 50));
        return searchOrchestrationService.searchAll(query, display, sort, period);
    }

    @GetMapping("/trending")
    public Map<String, Object> trending(
            @RequestParam(defaultValue = "10") int display,
            @RequestParam(required = false) String categoryId
    ) {
        display = Math.max(1, Math.min(display, 50));
        if (categoryId != null && !categoryId.isEmpty()) {
            return searchOrchestrationService.trendingByCategory(display, categoryId);
        }
        return searchOrchestrationService.trending(display);
    }
}
