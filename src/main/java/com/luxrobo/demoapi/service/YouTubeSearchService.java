package com.luxrobo.demoapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class YouTubeSearchService {

    @Value("${youtube.api-key}")
    private String apiKey;

    private final HttpClientService httpClientService;

    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3";

    public YouTubeSearchService(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    public String search(String query, int maxResults, String sort) throws Exception {
        return search(query, maxResults, sort, "all");
    }

    public String search(String query, int maxResults, String sort, String period) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String ytOrder = "date".equals(sort) ? "date" : "relevance";
        String urlStr = BASE_URL + "/search"
                + "?part=snippet"
                + "&q=" + encodedQuery
                + "&maxResults=" + maxResults
                + "&type=video"
                + "&regionCode=KR"
                + "&relevanceLanguage=ko"
                + "&order=" + ytOrder
                + publishedAfterParam(period)
                + "&key=" + apiKey;

        return httpClientService.get(urlStr, Map.of());
    }

    public String searchShorts(String query, int maxResults, String sort) throws Exception {
        return searchShorts(query, maxResults, sort, "all");
    }

    public String searchShorts(String query, int maxResults, String sort, String period) throws Exception {
        String encodedQuery = URLEncoder.encode(query + " #shorts", StandardCharsets.UTF_8);
        String ytOrder = "date".equals(sort) ? "date" : "relevance";
        String urlStr = BASE_URL + "/search"
                + "?part=snippet"
                + "&q=" + encodedQuery
                + "&maxResults=" + maxResults
                + "&type=video"
                + "&videoDuration=short"
                + "&regionCode=KR"
                + "&relevanceLanguage=ko"
                + "&order=" + ytOrder
                + publishedAfterParam(period)
                + "&key=" + apiKey;

        return httpClientService.get(urlStr, Map.of());
    }

    private String publishedAfterParam(String period) {
        if (period == null || "all".equals(period)) return "";
        java.time.Instant after;
        switch (period) {
            case "1d": after = java.time.Instant.now().minus(java.time.Duration.ofDays(1)); break;
            case "1w": after = java.time.Instant.now().minus(java.time.Duration.ofDays(7)); break;
            case "1m": after = java.time.Instant.now().minus(java.time.Duration.ofDays(30)); break;
            default: return "";
        }
        return "&publishedAfter=" + after.toString();
    }

    public String trendingShorts(int maxResults) throws Exception {
        String encodedQuery = URLEncoder.encode("#shorts", StandardCharsets.UTF_8);
        String urlStr = BASE_URL + "/search"
                + "?part=snippet"
                + "&q=" + encodedQuery
                + "&maxResults=" + maxResults
                + "&type=video"
                + "&videoDuration=short"
                + "&regionCode=KR"
                + "&relevanceLanguage=ko"
                + "&order=viewCount"
                + "&publishedAfter=" + java.time.Instant.now().minus(java.time.Duration.ofDays(7)).toString()
                + "&key=" + apiKey;

        return httpClientService.get(urlStr, Map.of());
    }

    public String trending(int maxResults) throws Exception {
        return trending(maxResults, null);
    }

    public String trending(int maxResults, String videoCategoryId) throws Exception {
        String urlStr = BASE_URL + "/videos"
                + "?part=snippet,statistics"
                + "&chart=mostPopular"
                + "&regionCode=KR"
                + "&maxResults=" + maxResults
                + (videoCategoryId != null && !videoCategoryId.isEmpty()
                        ? "&videoCategoryId=" + videoCategoryId : "")
                + "&key=" + apiKey;

        return httpClientService.get(urlStr, Map.of());
    }
}
