package com.luxrobo.demoapi.controller;

import com.luxrobo.demoapi.service.InstagramSearchService;
import com.luxrobo.demoapi.service.NaverSearchService;
import com.luxrobo.demoapi.service.RedditSearchService;
import com.luxrobo.demoapi.service.YouTubeSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final NaverSearchService naverSearchService;
    private final YouTubeSearchService youTubeSearchService;
    private final RedditSearchService redditSearchService;
    private final InstagramSearchService instagramSearchService;

    public SearchController(NaverSearchService naverSearchService, YouTubeSearchService youTubeSearchService, RedditSearchService redditSearchService, InstagramSearchService instagramSearchService) {
        this.naverSearchService = naverSearchService;
        this.youTubeSearchService = youTubeSearchService;
        this.redditSearchService = redditSearchService;
        this.instagramSearchService = instagramSearchService;
    }

    @GetMapping("/{category}")
    public String searchByCategory(
            @PathVariable String category,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int display,
            @RequestParam(defaultValue = "1") int start,
            @RequestParam(defaultValue = "sim") String sort
    ) throws Exception {
        if ("youtube".equals(category)) {
            return youTubeSearchService.search(query, display);
        }
        if ("shorts".equals(category)) {
            return youTubeSearchService.searchShorts(query, display);
        }
        if ("reddit".equals(category)) {
            return redditSearchService.search(query, display);
        }
        if ("instagram".equals(category)) {
            return instagramSearchService.searchByHashtag(query, display);
        }
        return naverSearchService.search(category, query, display, start, sort);
    }

    @GetMapping("/all")
    public Map<String, Object> searchAll(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int display
    ) {
        Map<String, CompletableFuture<String>> futures = naverSearchService.searchAll(query, display);
        Map<String, Object> results = new HashMap<>();

        // Naver results
        futures.forEach((category, future) -> {
            try {
                results.put(category, future.get());
            } catch (Exception e) {
                results.put(category, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        // YouTube results
        try {
            results.put("youtube", youTubeSearchService.search(query, display));
        } catch (Exception e) {
            results.put("youtube", "{\"error\":\"" + e.getMessage() + "\"}");
        }

        // YouTube Shorts
        try {
            results.put("shorts", youTubeSearchService.searchShorts(query, display));
        } catch (Exception e) {
            results.put("shorts", "{\"error\":\"" + e.getMessage() + "\"}");
        }

        // Reddit results
        try {
            results.put("reddit", redditSearchService.search(query, display));
        } catch (Exception e) {
            results.put("reddit", "{\"error\":\"" + e.getMessage() + "\"}");
        }

        // Instagram results
        try {
            results.put("instagram", instagramSearchService.searchByHashtag(query, display));
        } catch (Exception e) {
            results.put("instagram", "{\"data\":[]}");
        }

        return results;
    }

    @GetMapping("/trending")
    public Map<String, Object> trending(
            @RequestParam(defaultValue = "10") int display
    ) {
        Map<String, Object> results = new HashMap<>();
        String[] trendingKeywords = {"맛집", "여행", "IT", "영화", "음악"};

        // YouTube trending
        CompletableFuture<String> ytTrending = CompletableFuture.supplyAsync(() -> {
            try {
                return youTubeSearchService.trending(display);
            } catch (Exception e) {
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

        // Naver trending - popular topics
        CompletableFuture<String> naverNews = CompletableFuture.supplyAsync(() -> {
            try {
                return naverSearchService.search("news", "", display, 1, "date");
            } catch (Exception e) {
                return "{\"items\":[]}";
            }
        });

        // Naver blog trending with random keyword
        String keyword = trendingKeywords[(int) (Math.random() * trendingKeywords.length)];
        CompletableFuture<String> naverBlog = CompletableFuture.supplyAsync(() -> {
            try {
                return naverSearchService.search("blog", keyword, display, 1, "date");
            } catch (Exception e) {
                return "{\"items\":[]}";
            }
        });

        CompletableFuture<String> naverShop = CompletableFuture.supplyAsync(() -> {
            try {
                return naverSearchService.search("shop", keyword, display, 1, "date");
            } catch (Exception e) {
                return "{\"items\":[]}";
            }
        });

        // YouTube Shorts trending
        CompletableFuture<String> ytShorts = CompletableFuture.supplyAsync(() -> {
            try {
                return youTubeSearchService.trendingShorts(display);
            } catch (Exception e) {
                return "{\"items\":[]}";
            }
        });

        // Reddit trending
        CompletableFuture<String> redditTrending = CompletableFuture.supplyAsync(() -> {
            try {
                return redditSearchService.trending(display);
            } catch (Exception e) {
                return "{\"data\":{\"children\":[]}}";
            }
        });

        // Instagram trending
        CompletableFuture<String> instaTrending = CompletableFuture.supplyAsync(() -> {
            try {
                return instagramSearchService.searchByHashtag(keyword, display);
            } catch (Exception e) {
                return "{\"data\":[]}";
            }
        });

        try {
            results.put("youtube", ytTrending.get());
            results.put("news", naverNews.get());
            results.put("blog", naverBlog.get());
            results.put("shop", naverShop.get());
            results.put("shorts", ytShorts.get());
            results.put("reddit", redditTrending.get());
            results.put("instagram", instaTrending.get());
            results.put("keyword", keyword);
        } catch (Exception e) {
            results.put("error", e.getMessage());
        }

        return results;
    }
}
