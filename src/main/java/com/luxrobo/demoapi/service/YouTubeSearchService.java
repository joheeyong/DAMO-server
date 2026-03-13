package com.luxrobo.demoapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class YouTubeSearchService {

    @Value("${youtube.api-key}")
    private String apiKey;

    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3";

    public String search(String query, int maxResults, String sort) throws Exception {
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
                + "&key=" + apiKey;

        return fetchUrl(urlStr);
    }

    public String searchShorts(String query, int maxResults, String sort) throws Exception {
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
                + "&key=" + apiKey;

        return fetchUrl(urlStr);
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

        return fetchUrl(urlStr);
    }

    public String trending(int maxResults) throws Exception {
        String urlStr = BASE_URL + "/videos"
                + "?part=snippet,statistics"
                + "&chart=mostPopular"
                + "&regionCode=KR"
                + "&maxResults=" + maxResults
                + "&key=" + apiKey;

        return fetchUrl(urlStr);
    }

    private String fetchUrl(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

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
