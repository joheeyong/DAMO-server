package com.luxrobo.demoapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NaverSearchService {

    @Value("${naver.search.client-id}")
    private String clientId;

    @Value("${naver.search.client-secret}")
    private String clientSecret;

    private final HttpClientService httpClientService;

    private static final String BASE_URL = "https://openapi.naver.com/v1/search";

    private static final Map<String, String> CATEGORY_PATHS = Map.of(
            "blog", "/blog.json",
            "news", "/news.json",
            "cafe", "/cafearticle.json",
            "shop", "/shop.json",
            "image", "/image.json",
            "kin", "/kin.json",
            "book", "/book.json",
            "webkr", "/webkr.json"
    );

    public NaverSearchService(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    public String search(String category, String query, int display, int start, String sort) throws Exception {
        String path = CATEGORY_PATHS.getOrDefault(category, "/blog.json");
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        String urlStr = BASE_URL + path
                + "?query=" + encodedQuery
                + "&display=" + display
                + "&start=" + start
                + "&sort=" + sort;

        return httpClientService.get(urlStr, Map.of(
                "X-Naver-Client-Id", clientId,
                "X-Naver-Client-Secret", clientSecret
        ));
    }

    public Map<String, CompletableFuture<String>> searchAll(String query, int display, String sort) {
        Map<String, CompletableFuture<String>> results = new ConcurrentHashMap<>();

        for (String category : CATEGORY_PATHS.keySet()) {
            results.put(category, CompletableFuture.supplyAsync(() -> {
                try {
                    return search(category, query, display, 1, sort);
                } catch (Exception e) {
                    return "{\"error\":\"Search request failed\"}";
                }
            }));
        }

        return results;
    }
}
