package com.luxrobo.demoapi.controller;

import com.luxrobo.demoapi.service.SocialFeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/social")
public class SocialFeedController {

    private final SocialFeedService socialFeedService;

    public SocialFeedController(SocialFeedService socialFeedService) {
        this.socialFeedService = socialFeedService;
    }

    @PostMapping("/posts")
    public ResponseEntity<?> createPost(@AuthenticationPrincipal Long userId,
                                        @RequestBody Map<String, Object> body) {
        String content = (String) body.getOrDefault("content", "");
        if (content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }
        String images = body.get("images") != null ? body.get("images").toString() : "[]";
        return ResponseEntity.ok(socialFeedService.createPost(userId, content.trim(), images));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<?> deletePost(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long id) {
        socialFeedService.deletePost(userId, id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<?> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(socialFeedService.getPost(id));
    }

    @GetMapping("/feed")
    public ResponseEntity<?> getFeed(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(socialFeedService.getFeed(page, size));
    }

    @GetMapping("/posts/me")
    public ResponseEntity<?> getMyPosts(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(socialFeedService.getMyPosts(userId));
    }

    // --- Media upload (images + videos) ---

    @PostMapping("/media/upload")
    public ResponseEntity<?> uploadMedia(@AuthenticationPrincipal Long userId,
                                         @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(socialFeedService.uploadMedia(userId, file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- Comments ---

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(socialFeedService.getComments(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<?> addComment(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long postId,
                                        @RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }
        return ResponseEntity.ok(socialFeedService.addComment(userId, postId, content));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@AuthenticationPrincipal Long userId,
                                           @PathVariable Long commentId) {
        socialFeedService.deleteComment(userId, commentId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // --- Likes ---

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<?> toggleLike(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long postId) {
        return ResponseEntity.ok(socialFeedService.toggleLike(userId, postId));
    }

    @GetMapping("/posts/{postId}/like")
    public ResponseEntity<?> checkLike(@AuthenticationPrincipal Long userId,
                                       @PathVariable Long postId) {
        return ResponseEntity.ok(socialFeedService.checkLike(userId, postId));
    }
}
