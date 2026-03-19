package com.luxrobo.demoapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class InstagramSearchService {

    @Value("${instagram.access-token:}")
    private String accessToken;

    @Value("${instagram.user-id:}")
    private String userId;

    private final HttpClientService httpClientService;

    private static final String GRAPH_URL = "https://graph.facebook.com/v19.0";

    public InstagramSearchService(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    /**
     * Search hashtag and get recent media
     * Step 1: Get hashtag ID
     * Step 2: Get recent media for that hashtag
     */
    public String searchByHashtag(String hashtag, int limit) throws Exception {
        if (accessToken.isEmpty() || userId.isEmpty()) {
            return "{\"data\":[]}";
        }

        // Step 1: Get hashtag ID
        String hashtagId = getHashtagId(hashtag);
        if (hashtagId == null) {
            return "{\"data\":[]}";
        }

        // Step 2: Get recent media
        String urlStr = GRAPH_URL + "/" + hashtagId + "/recent_media"
                + "?user_id=" + userId
                + "&fields=id,caption,media_type,media_url,permalink,thumbnail_url,timestamp,like_count,comments_count"
                + "&limit=" + limit
                + "&access_token=" + accessToken;

        return httpClientService.get(urlStr, Map.of());
    }

    private String getHashtagId(String hashtag) throws Exception {
        String encoded = URLEncoder.encode(hashtag, StandardCharsets.UTF_8);
        String urlStr = GRAPH_URL + "/ig_hashtag_search"
                + "?q=" + encoded
                + "&user_id=" + userId
                + "&access_token=" + accessToken;

        String response = httpClientService.get(urlStr, Map.of());
        // Extract first hashtag ID from response
        // Response format: {"data":[{"id":"17843853986012965"}]}
        int idStart = response.indexOf("\"id\":\"");
        if (idStart == -1) return null;
        idStart += 6;
        int idEnd = response.indexOf("\"", idStart);
        return response.substring(idStart, idEnd);
    }
}
