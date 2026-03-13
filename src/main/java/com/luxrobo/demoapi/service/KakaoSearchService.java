package com.luxrobo.demoapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KakaoSearchService {

    @Value("${kakao.rest-api-key:}")
    private String restApiKey;

    private static final String BASE_URL = "https://dapi.kakao.com/v2/search";

    private static final Map<String, String> CATEGORY_PATHS = Map.of(
            "kakao-blog", "/blog",
            "kakao-cafe", "/cafe",
            "kakao-web", "/web",
            "kakao-video", "/vclip",
            "kakao-image", "/image"
    );

    public String search(String category, String query, int size, int page, String sort) throws Exception {
        if (restApiKey.isEmpty()) {
            return "{\"documents\":[]}";
        }

        String path = CATEGORY_PATHS.getOrDefault(category, "/web");
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        String urlStr = BASE_URL + path
                + "?query=" + encodedQuery
                + "&size=" + size
                + "&page=" + page
                + "&sort=" + sort;

        return fetchUrl(urlStr);
    }

    public Map<String, CompletableFuture<String>> searchAll(String query, int size, String sort) {
        Map<String, CompletableFuture<String>> results = new ConcurrentHashMap<>();

        if (restApiKey.isEmpty()) {
            return results;
        }

        String kakaoSort = "date".equals(sort) ? "recency" : "accuracy";
        for (String category : CATEGORY_PATHS.keySet()) {
            results.put(category, CompletableFuture.supplyAsync(() -> {
                try {
                    return search(category, query, size, 1, kakaoSort);
                } catch (Exception e) {
                    return "{\"documents\":[]}";
                }
            }));
        }

        return results;
    }

    private String fetchUrl(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "KakaoAK " + restApiKey);

        int responseCode = conn.getResponseCode();
        BufferedReader br;
        if (responseCode == 200) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        conn.disconnect();

        return sb.toString();
    }
}
