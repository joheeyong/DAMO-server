package com.luxrobo.demoapi.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class SuggestionService {

    public List<String> getSuggestions(String query) {
        List<String> suggestions = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String urlStr = "https://ac.search.naver.com/nx/ac?q=" + encoded
                    + "&con=1&frm=nv&ans=2&r_format=json&r_enc=UTF-8&r_unicode=0&t_koreng=1&run=2&rev=4&q_enc=UTF-8&st=100";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                // Parse: {"items":[[["keyword","0"],["keyword2","0"]]]}
                String json = sb.toString();
                int itemsStart = json.indexOf("\"items\"");
                if (itemsStart >= 0) {
                    int tripleStart = json.indexOf("[[[", itemsStart);
                    if (tripleStart >= 0) {
                        int tripleEnd = json.indexOf("]]]", tripleStart);
                        if (tripleEnd >= 0) {
                            String inner = json.substring(tripleStart + 2, tripleEnd + 1);
                            String[] entries = inner.split("\\],\\s*\\[");
                            for (String entry : entries) {
                                entry = entry.replaceAll("[\\[\\]]", "");
                                int firstQuote = entry.indexOf('"');
                                int secondQuote = entry.indexOf('"', firstQuote + 1);
                                if (firstQuote >= 0 && secondQuote > firstQuote) {
                                    String kw = entry.substring(firstQuote + 1, secondQuote).trim();
                                    if (!kw.isEmpty() && suggestions.size() < 10) {
                                        suggestions.add(kw);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            // Return empty suggestions on error
        }
        return suggestions;
    }
}
