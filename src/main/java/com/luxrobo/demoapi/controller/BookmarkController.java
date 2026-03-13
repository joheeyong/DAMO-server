package com.luxrobo.demoapi.controller;

import com.luxrobo.demoapi.entity.Bookmark;
import com.luxrobo.demoapi.repository.BookmarkRepository;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkRepository bookmarkRepository;

    public BookmarkController(BookmarkRepository bookmarkRepository) {
        this.bookmarkRepository = bookmarkRepository;
    }

    @GetMapping
    public List<Map<String, Object>> list(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return List.of();
        }
        Long userId = (Long) authentication.getPrincipal();
        List<Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return bookmarks.stream().map(this::toMap).toList();
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Map.of("status", "unauthorized");
        }
        Long userId = (Long) authentication.getPrincipal();
        String contentId = (String) body.get("contentId");

        if (bookmarkRepository.existsByUserIdAndContentId(userId, contentId)) {
            return Map.of("status", "already_exists");
        }

        Bookmark bookmark = new Bookmark();
        bookmark.setUserId(userId);
        bookmark.setContentId(contentId);
        bookmark.setPlatform((String) body.getOrDefault("platform", ""));
        bookmark.setTitle((String) body.getOrDefault("title", ""));
        bookmark.setDescription((String) body.getOrDefault("description", ""));
        bookmark.setLink((String) body.getOrDefault("link", ""));
        bookmark.setImage((String) body.getOrDefault("image", ""));
        bookmark.setAuthor((String) body.getOrDefault("author", ""));
        bookmark.setDate((String) body.getOrDefault("date", ""));

        Object extra = body.get("extra");
        if (extra != null) {
            try {
                bookmark.setExtraJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(extra));
            } catch (Exception e) {
                bookmark.setExtraJson(null);
            }
        }

        bookmarkRepository.save(bookmark);
        return Map.of("status", "added");
    }

    @DeleteMapping("/{contentId}")
    @Transactional
    public Map<String, Object> remove(@PathVariable String contentId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Map.of("status", "unauthorized");
        }
        Long userId = (Long) authentication.getPrincipal();
        bookmarkRepository.deleteByUserIdAndContentId(userId, contentId);
        return Map.of("status", "removed");
    }

    @PostMapping("/sync")
    @Transactional
    public Map<String, Object> sync(@RequestBody List<Map<String, Object>> items, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Map.of("status", "unauthorized");
        }
        Long userId = (Long) authentication.getPrincipal();

        // Merge: add items from localStorage that don't exist on server
        for (Map<String, Object> body : items) {
            String contentId = (String) body.get("id");
            if (contentId == null || bookmarkRepository.existsByUserIdAndContentId(userId, contentId)) continue;

            Bookmark bookmark = new Bookmark();
            bookmark.setUserId(userId);
            bookmark.setContentId(contentId);
            bookmark.setPlatform((String) body.getOrDefault("platform", ""));
            bookmark.setTitle((String) body.getOrDefault("title", ""));
            bookmark.setDescription((String) body.getOrDefault("description", ""));
            bookmark.setLink((String) body.getOrDefault("link", ""));
            bookmark.setImage((String) body.getOrDefault("image", ""));
            bookmark.setAuthor((String) body.getOrDefault("author", ""));
            bookmark.setDate((String) body.getOrDefault("date", ""));

            Object extra = body.get("extra");
            if (extra != null) {
                try {
                    bookmark.setExtraJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(extra));
                } catch (Exception e) {
                    bookmark.setExtraJson(null);
                }
            }

            bookmarkRepository.save(bookmark);
        }

        // Return full server list
        List<Bookmark> all = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return Map.of("status", "synced", "bookmarks", all.stream().map(this::toMap).toList());
    }

    private Map<String, Object> toMap(Bookmark b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getContentId());
        m.put("platform", b.getPlatform());
        m.put("title", b.getTitle());
        m.put("description", b.getDescription());
        m.put("link", b.getLink());
        m.put("image", b.getImage());
        m.put("author", b.getAuthor());
        m.put("date", b.getDate());
        m.put("savedAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
        if (b.getExtraJson() != null) {
            try {
                m.put("extra", new com.fasterxml.jackson.databind.ObjectMapper().readValue(b.getExtraJson(), Map.class));
            } catch (Exception e) {
                m.put("extra", null);
            }
        }
        return m;
    }
}
