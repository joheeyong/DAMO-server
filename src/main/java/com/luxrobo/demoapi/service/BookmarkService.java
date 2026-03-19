package com.luxrobo.demoapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luxrobo.demoapi.entity.Bookmark;
import com.luxrobo.demoapi.repository.BookmarkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final ObjectMapper objectMapper;

    public BookmarkService(BookmarkRepository bookmarkRepository, ObjectMapper objectMapper) {
        this.bookmarkRepository = bookmarkRepository;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> list(Long userId) {
        List<Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return bookmarks.stream().map(this::toResponse).toList();
    }

    public Map<String, Object> add(Long userId, Map<String, Object> body) {
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
                bookmark.setExtraJson(objectMapper.writeValueAsString(extra));
            } catch (Exception e) {
                bookmark.setExtraJson(null);
            }
        }

        bookmarkRepository.save(bookmark);
        return Map.of("status", "added");
    }

    @Transactional
    public Map<String, Object> remove(Long userId, String contentId) {
        bookmarkRepository.deleteByUserIdAndContentId(userId, contentId);
        return Map.of("status", "removed");
    }

    @Transactional
    public Map<String, Object> sync(Long userId, List<Map<String, Object>> items) {
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
                    bookmark.setExtraJson(objectMapper.writeValueAsString(extra));
                } catch (Exception e) {
                    bookmark.setExtraJson(null);
                }
            }

            bookmarkRepository.save(bookmark);
        }

        List<Bookmark> all = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return Map.of("status", "synced", "bookmarks", all.stream().map(this::toResponse).toList());
    }

    @Transactional
    public Map<String, Object> clearAll(Long userId) {
        List<Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);
        bookmarkRepository.deleteAll(bookmarks);
        return Map.of("status", "cleared");
    }

    public Map<String, Object> toResponse(Bookmark b) {
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
                m.put("extra", objectMapper.readValue(b.getExtraJson(), Map.class));
            } catch (Exception e) {
                m.put("extra", null);
            }
        }
        return m;
    }
}
