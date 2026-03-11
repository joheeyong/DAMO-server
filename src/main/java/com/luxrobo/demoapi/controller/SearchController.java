package com.luxrobo.demoapi.controller;

import com.luxrobo.demoapi.service.NaverSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final NaverSearchService naverSearchService;

    public SearchController(NaverSearchService naverSearchService) {
        this.naverSearchService = naverSearchService;
    }

    @GetMapping("/{category}")
    public String searchByCategory(
            @PathVariable String category,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int display,
            @RequestParam(defaultValue = "1") int start,
            @RequestParam(defaultValue = "sim") String sort
    ) throws Exception {
        return naverSearchService.search(category, query, display, start, sort);
    }

    @GetMapping("/all")
    public Map<String, Object> searchAll(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int display
    ) {
        Map<String, CompletableFuture<String>> futures = naverSearchService.searchAll(query, display);
        Map<String, Object> results = new HashMap<>();

        futures.forEach((category, future) -> {
            try {
                results.put(category, future.get());
            } catch (Exception e) {
                results.put(category, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        return results;
    }
}
