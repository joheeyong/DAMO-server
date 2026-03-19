package com.luxrobo.demoapi.controller;

import com.luxrobo.demoapi.service.BookmarkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(bookmarkService.list(userId));
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Map<String, Object> body, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(bookmarkService.add(userId, body));
    }

    @DeleteMapping("/{contentId}")
    public ResponseEntity<?> remove(@PathVariable String contentId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(bookmarkService.remove(userId, contentId));
    }

    @PostMapping("/sync")
    public ResponseEntity<?> sync(@RequestBody List<Map<String, Object>> items, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(bookmarkService.sync(userId, items));
    }

    @DeleteMapping
    public ResponseEntity<?> clearAll(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(bookmarkService.clearAll(userId));
    }
}
