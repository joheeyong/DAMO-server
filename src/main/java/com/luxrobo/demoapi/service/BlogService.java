package com.luxrobo.demoapi.service;

import com.luxrobo.demoapi.entity.*;
import com.luxrobo.demoapi.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BlogService {

    private final BlogPostRepository postRepo;
    private final BlogCommentRepository commentRepo;
    private final BlogLikeRepository likeRepo;
    private final UserRepository userRepo;

    // Local upload directory (EC2 filesystem, served via Nginx)
    private static final String UPLOAD_DIR = "/home/ec2-user/uploads/blog";
    private static final String UPLOAD_URL_PREFIX = "/uploads/blog";

    public BlogService(BlogPostRepository postRepo, BlogCommentRepository commentRepo,
                       BlogLikeRepository likeRepo, UserRepository userRepo) {
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
        this.likeRepo = likeRepo;
        this.userRepo = userRepo;
    }

    // --- Posts ---

    @Transactional
    public BlogPost createPost(Long userId, Map<String, Object> body) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        BlogPost post = new BlogPost();
        post.setUserId(userId);
        post.setTitle((String) body.getOrDefault("title", ""));
        post.setContent((String) body.getOrDefault("content", ""));
        post.setSummary(generateSummary((String) body.getOrDefault("content", "")));
        post.setCoverImage((String) body.get("coverImage"));
        post.setTags((String) body.get("tags"));
        post.setAuthorName(user.getName());
        post.setAuthorImage(user.getProfileImage());

        String status = (String) body.getOrDefault("status", "DRAFT");
        post.setStatus(status);
        if ("PUBLISHED".equals(status)) {
            post.setPublishedAt(LocalDateTime.now());
        }
        return postRepo.save(post);
    }

    @Transactional
    public BlogPost updatePost(Long userId, Long postId, Map<String, Object> body) {
        BlogPost post = postRepo.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        if (!post.getUserId().equals(userId)) throw new RuntimeException("Unauthorized");

        if (body.containsKey("title")) post.setTitle((String) body.get("title"));
        if (body.containsKey("content")) {
            post.setContent((String) body.get("content"));
            post.setSummary(generateSummary((String) body.get("content")));
        }
        if (body.containsKey("coverImage")) post.setCoverImage((String) body.get("coverImage"));
        if (body.containsKey("tags")) post.setTags((String) body.get("tags"));
        if (body.containsKey("status")) {
            String newStatus = (String) body.get("status");
            if ("PUBLISHED".equals(newStatus) && !"PUBLISHED".equals(post.getStatus())) {
                post.setPublishedAt(LocalDateTime.now());
            }
            post.setStatus(newStatus);
        }
        return postRepo.save(post);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        BlogPost post = postRepo.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        if (!post.getUserId().equals(userId)) throw new RuntimeException("Unauthorized");
        postRepo.delete(post);
    }

    @Transactional
    public Map<String, Object> getPost(Long postId) {
        BlogPost post = postRepo.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        postRepo.incrementViewCount(postId);
        long likeCount = likeRepo.countByPostId(postId);
        long commentCount = commentRepo.countByPostId(postId);
        return toDetailMap(post, likeCount, commentCount);
    }

    public List<Map<String, Object>> getMyPosts(Long userId) {
        return postRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("title", p.getTitle());
                    m.put("summary", p.getSummary());
                    m.put("coverImage", p.getCoverImage());
                    m.put("status", p.getStatus());
                    m.put("viewCount", p.getViewCount());
                    m.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
                    m.put("publishedAt", p.getPublishedAt() != null ? p.getPublishedAt().toString() : null);
                    m.put("likeCount", likeRepo.countByPostId(p.getId()));
                    m.put("commentCount", commentRepo.countByPostId(p.getId()));
                    return m;
                })
                .collect(Collectors.toList());
    }

    // --- Feed integration ---

    public List<Map<String, Object>> getFeedItems(int page, int size) {
        return postRepo.findByStatusOrderByPublishedAtDesc("PUBLISHED", PageRequest.of(page, size))
                .getContent().stream()
                .map(this::toFeedItem)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> searchPosts(String query) {
        return postRepo.searchPublished(query).stream()
                .map(this::toFeedItem)
                .collect(Collectors.toList());
    }

    /** Returns JSON-compatible structure for SearchOrchestrationService */
    public String toFeedJson(int limit) {
        List<BlogPost> posts = postRepo.findTop10ByStatusOrderByPublishedAtDesc("PUBLISHED");
        if (posts.isEmpty()) return "{\"items\":[]}";
        StringBuilder sb = new StringBuilder("{\"items\":[");
        for (int i = 0; i < Math.min(limit, posts.size()); i++) {
            if (i > 0) sb.append(",");
            BlogPost p = posts.get(i);
            long likes = likeRepo.countByPostId(p.getId());
            long comments = commentRepo.countByPostId(p.getId());
            sb.append("{")
              .append("\"id\":").append(p.getId()).append(",")
              .append("\"title\":\"").append(escapeJson(p.getTitle())).append("\",")
              .append("\"summary\":\"").append(escapeJson(p.getSummary() != null ? p.getSummary() : "")).append("\",")
              .append("\"coverImage\":\"").append(escapeJson(p.getCoverImage() != null ? p.getCoverImage() : "")).append("\",")
              .append("\"authorName\":\"").append(escapeJson(p.getAuthorName() != null ? p.getAuthorName() : "")).append("\",")
              .append("\"authorImage\":\"").append(escapeJson(p.getAuthorImage() != null ? p.getAuthorImage() : "")).append("\",")
              .append("\"publishedAt\":\"").append(p.getPublishedAt() != null ? p.getPublishedAt().toString() : "").append("\",")
              .append("\"likeCount\":").append(likes).append(",")
              .append("\"commentCount\":").append(comments)
              .append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    public String searchToFeedJson(String query, int limit) {
        List<BlogPost> posts = postRepo.searchPublished(query);
        if (posts.isEmpty()) return "{\"items\":[]}";
        StringBuilder sb = new StringBuilder("{\"items\":[");
        int count = 0;
        for (BlogPost p : posts) {
            if (count >= limit) break;
            if (count > 0) sb.append(",");
            long likes = likeRepo.countByPostId(p.getId());
            long comments = commentRepo.countByPostId(p.getId());
            sb.append("{")
              .append("\"id\":").append(p.getId()).append(",")
              .append("\"title\":\"").append(escapeJson(p.getTitle())).append("\",")
              .append("\"summary\":\"").append(escapeJson(p.getSummary() != null ? p.getSummary() : "")).append("\",")
              .append("\"coverImage\":\"").append(escapeJson(p.getCoverImage() != null ? p.getCoverImage() : "")).append("\",")
              .append("\"authorName\":\"").append(escapeJson(p.getAuthorName() != null ? p.getAuthorName() : "")).append("\",")
              .append("\"authorImage\":\"").append(escapeJson(p.getAuthorImage() != null ? p.getAuthorImage() : "")).append("\",")
              .append("\"publishedAt\":\"").append(p.getPublishedAt() != null ? p.getPublishedAt().toString() : "").append("\",")
              .append("\"likeCount\":").append(likes).append(",")
              .append("\"commentCount\":").append(comments)
              .append("}");
            count++;
        }
        sb.append("]}");
        return sb.toString();
    }

    // --- Comments ---

    @Transactional
    public BlogComment addComment(Long userId, Long postId, String content) {
        if (!postRepo.existsById(postId)) throw new RuntimeException("Post not found");
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        BlogComment comment = new BlogComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setAuthorName(user.getName());
        comment.setAuthorImage(user.getProfileImage());
        return commentRepo.save(comment);
    }

    public List<BlogComment> getComments(Long postId) {
        return commentRepo.findByPostIdOrderByCreatedAtAsc(postId);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        BlogComment comment = commentRepo.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found"));
        if (!comment.getUserId().equals(userId)) throw new RuntimeException("Unauthorized");
        commentRepo.delete(comment);
    }

    // --- Likes ---

    @Transactional
    public Map<String, Object> toggleLike(Long userId, Long postId) {
        Optional<BlogLike> existing = likeRepo.findByPostIdAndUserId(postId, userId);
        boolean liked;
        if (existing.isPresent()) {
            likeRepo.delete(existing.get());
            liked = false;
        } else {
            BlogLike like = new BlogLike();
            like.setPostId(postId);
            like.setUserId(userId);
            likeRepo.save(like);
            liked = true;
        }
        long count = likeRepo.countByPostId(postId);
        return Map.of("liked", liked, "likeCount", count);
    }

    public Map<String, Object> checkLike(Long userId, Long postId) {
        boolean liked = likeRepo.existsByPostIdAndUserId(postId, userId);
        long count = likeRepo.countByPostId(postId);
        return Map.of("liked", liked, "likeCount", count);
    }

    // --- Image Upload ---

    public Map<String, String> uploadImage(Long userId, MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new RuntimeException("File is empty");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("File size exceeds 5MB limit");
        }

        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + ext;
        String userDir = UPLOAD_DIR + "/" + userId;
        Files.createDirectories(Paths.get(userDir));
        Path dest = Paths.get(userDir, filename);
        file.transferTo(dest.toFile());

        String url = UPLOAD_URL_PREFIX + "/" + userId + "/" + filename;
        return Map.of("url", url);
    }

    // --- Helpers ---

    private String generateSummary(String htmlContent) {
        if (htmlContent == null) return "";
        String text = htmlContent.replaceAll("<[^>]*>", "").replaceAll("&nbsp;", " ").trim();
        return text.length() > 200 ? text.substring(0, 200) : text;
    }

    private Map<String, Object> toFeedItem(BlogPost p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("title", p.getTitle());
        m.put("summary", p.getSummary());
        m.put("coverImage", p.getCoverImage());
        m.put("authorName", p.getAuthorName());
        m.put("authorImage", p.getAuthorImage());
        m.put("publishedAt", p.getPublishedAt() != null ? p.getPublishedAt().toString() : null);
        m.put("likeCount", likeRepo.countByPostId(p.getId()));
        m.put("commentCount", commentRepo.countByPostId(p.getId()));
        return m;
    }

    private Map<String, Object> toDetailMap(BlogPost p, long likeCount, long commentCount) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("userId", p.getUserId());
        m.put("title", p.getTitle());
        m.put("summary", p.getSummary());
        m.put("content", p.getContent());
        m.put("coverImage", p.getCoverImage());
        m.put("status", p.getStatus());
        m.put("tags", p.getTags());
        m.put("viewCount", p.getViewCount());
        m.put("authorName", p.getAuthorName());
        m.put("authorImage", p.getAuthorImage());
        m.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        m.put("publishedAt", p.getPublishedAt() != null ? p.getPublishedAt().toString() : null);
        m.put("likeCount", likeCount);
        m.put("commentCount", commentCount);
        return m;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
