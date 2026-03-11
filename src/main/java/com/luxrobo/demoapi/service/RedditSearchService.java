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
    private static final String USER_AGENT = "DAMO/1.0";

    public String search(String query, int limit) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String urlStr = BASE_URL + "/search.json"
                + "?q=" + encodedQuery
                + "&limit=" + limit
                + "&sort=relevance"
                + "&t=week";

        return fetchUrl(urlStr);
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
