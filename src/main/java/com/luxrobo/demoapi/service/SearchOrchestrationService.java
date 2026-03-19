package com.luxrobo.demoapi.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class SearchOrchestrationService {

    private final NaverSearchService naverSearchService;
    private final YouTubeSearchService youTubeSearchService;
    private final InstagramSearchService instagramSearchService;
    private final KakaoSearchService kakaoSearchService;

    public SearchOrchestrationService(NaverSearchService naverSearchService,
                                       YouTubeSearchService youTubeSearchService,
                                       InstagramSearchService instagramSearchService,
                                       KakaoSearchService kakaoSearchService) {
        this.naverSearchService = naverSearchService;
        this.youTubeSearchService = youTubeSearchService;
        this.instagramSearchService = instagramSearchService;
        this.kakaoSearchService = kakaoSearchService;
    }

    public Map<String, Object> searchAll(String query, int display, String sort, String period) {
        Map<String, CompletableFuture<String>> futures = naverSearchService.searchAll(query, display, sort);
        Map<String, Object> results = new HashMap<>();

        // Naver results
        futures.forEach((category, future) -> {
            try {
                results.put(category, future.get());
            } catch (Exception e) {
                results.put(category, "{\"error\":\"Search request failed\"}");
            }
        });

        // YouTube results (supports publishedAfter)
        try {
            results.put("youtube", youTubeSearchService.search(query, display, sort, period));
        } catch (Exception e) {
            results.put("youtube", "{\"error\":\"Search request failed\"}");
        }

        // YouTube Shorts (supports publishedAfter)
        try {
            results.put("shorts", youTubeSearchService.searchShorts(query, display, sort, period));
        } catch (Exception e) {
            results.put("shorts", "{\"error\":\"Search request failed\"}");
        }

        // Instagram results
        try {
            results.put("instagram", instagramSearchService.searchByHashtag(query, display));
        } catch (Exception e) {
            results.put("instagram", "{\"data\":[]}");
        }

        // Kakao (Daum) results
        Map<String, CompletableFuture<String>> kakaoFutures = kakaoSearchService.searchAll(query, display, sort);
        kakaoFutures.forEach((category, future) -> {
            try {
                results.put(category, future.get());
            } catch (Exception e) {
                results.put(category, "{\"documents\":[]}");
            }
        });

        // Pass period info so frontend can filter Naver/Kakao client-side
        results.put("_period", period);

        return results;
    }

    public Map<String, Object> trending(int display) {
        Map<String, Object> results = new HashMap<>();
        String[] trendingKeywords = {"맛집", "여행", "IT", "영화", "음악"};

        // YouTube trending
        CompletableFuture<String> ytTrending = CompletableFuture.supplyAsync(() -> {
            try {
                return youTubeSearchService.trending(display);
            } catch (Exception e) {
                return "{\"error\":\"Search request failed\"}";
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

        // Kakao blog trending
        CompletableFuture<String> kakaoBlog = CompletableFuture.supplyAsync(() -> {
            try {
                return kakaoSearchService.search("kakao-blog", keyword, display, 1, "recency");
            } catch (Exception e) {
                return "{\"documents\":[]}";
            }
        });

        // Kakao cafe trending
        CompletableFuture<String> kakaoCafe = CompletableFuture.supplyAsync(() -> {
            try {
                return kakaoSearchService.search("kakao-cafe", keyword, display, 1, "recency");
            } catch (Exception e) {
                return "{\"documents\":[]}";
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
            results.put("instagram", instaTrending.get());
            results.put("kakao-blog", kakaoBlog.get());
            results.put("kakao-cafe", kakaoCafe.get());
            results.put("keyword", keyword);
        } catch (Exception e) {
            results.put("error", "Failed to fetch trending data");
        }

        return results;
    }

    public Map<String, Object> trendingByCategory(int display, String categoryId) {
        Map<String, Object> results = new HashMap<>();

        // YouTube trending by category (e.g., Sports=17, Music=10, Gaming=20)
        CompletableFuture<String> ytCategory = CompletableFuture.supplyAsync(() -> {
            try {
                return youTubeSearchService.trending(display, categoryId);
            } catch (Exception e) {
                return "{\"items\":[]}";
            }
        });

        // YouTube Shorts trending (general - no category filter for shorts)
        CompletableFuture<String> ytShorts = CompletableFuture.supplyAsync(() -> {
            try {
                return youTubeSearchService.trendingShorts(display);
            } catch (Exception e) {
                return "{\"items\":[]}";
            }
        });

        try {
            results.put("youtube", ytCategory.get());
            results.put("shorts", ytShorts.get());
        } catch (Exception e) {
            results.put("error", "Failed to fetch category trending data");
        }

        return results;
    }
}
