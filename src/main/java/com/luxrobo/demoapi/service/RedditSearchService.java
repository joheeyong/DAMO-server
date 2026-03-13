package com.luxrobo.demoapi.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class RedditSearchService {

    private static final String BASE_URL = "https://www.reddit.com";
    private static final String USER_AGENT = "web:com.damo.app:v1.0.0 (by /u/damo_search)";

    public String search(String query, int limit, String sort) throws Exception {
        return search(query, limit, sort, "all");
    }

    public String search(String query, int limit, String sort, String period) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String redditSort = "date".equals(sort) ? "new" : "relevance";
        String redditTime = toRedditTime(period);
        String urlStr = BASE_URL + "/search.json"
                + "?q=" + encodedQuery
                + "&limit=" + limit
                + "&sort=" + redditSort
                + "&t=" + redditTime;

        return fetchUrl(urlStr);
    }

    private String toRedditTime(String period) {
        if (period == null) return "all";
        switch (period) {
            case "1d": return "day";
            case "1w": return "week";
            case "1m": return "month";
            default: return "all";
        }
    }

    public String trending(int limit) throws Exception {
        String urlStr = BASE_URL + "/r/popular.json"
                + "?limit=" + limit
                + "&t=day";

        return fetchUrl(urlStr);
    }

    public String searchSubreddit(String subreddit, String query, int limit) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String urlStr = BASE_URL + "/r/" + subreddit + "/search.json"
                + "?q=" + encodedQuery
                + "&restrict_sr=on"
                + "&limit=" + limit
                + "&sort=relevance";

        return fetchUrl(urlStr);
    }

    private String fetchUrl(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            conn.disconnect();
            return "{\"data\":{\"children\":[]}}";
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        conn.disconnect();

        String result = sb.toString();
        if (!result.startsWith("{")) {
            return "{\"data\":{\"children\":[]}}";
        }
        return result;
    }
}
