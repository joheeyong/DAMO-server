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
public class SocialFeedService {

    private final SocialPostRepository postRepo;
    private final SocialCommentRepository commentRepo;
    private final SocialLikeRepository likeRepo;
    private final UserRepository userRepo;

    private static final String UPLOAD_DIR = "/home/ec2-user/uploads/social";
    private static final String UPLOAD_URL_PREFIX = "/uploads/social";
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024; // 50MB

    public SocialFeedService(SocialPostRepository postRepo, SocialCommentRepository commentRepo,
                             SocialLikeRepository likeRepo, UserRepository userRepo) {
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
        this.likeRepo = likeRepo;
        this.userRepo = userRepo;
    }

    // --- Posts ---

    @Transactional
    public Map<String, Object> createPost(Long userId, String content, String imagesJson) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        SocialPost post = new SocialPost();
        post.setUserId(userId);
        post.setContent(content);
        post.setImages(imagesJson);
        post.setAuthorName(user.getName());
        post.setAuthorImage(user.getProfileImage());
        post = postRepo.save(post);
        return toDetailMap(post, 0, 0);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        SocialPost post = postRepo.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        if (!post.getUserId().equals(userId)) throw new RuntimeException("Unauthorized");
        postRepo.delete(post);
    }

    public Map<String, Object> getPost(Long postId) {
        SocialPost post = postRepo.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        long likeCount = likeRepo.countByPostId(postId);
        long commentCount = commentRepo.countByPostId(postId);
        return toDetailMap(post, likeCount, commentCount);
    }

    public List<Map<String, Object>> getFeed(int page, int size) {
        return postRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .getContent().stream()
                .map(this::toFeedItem)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getMyPosts(Long userId) {
        return postRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toFeedItem)
                .collect(Collectors.toList());
    }

    // --- Feed integration for SearchOrchestrationService ---

    public String toFeedJson(int limit) {
        List<SocialPost> posts = postRepo.findTop20ByOrderByCreatedAtDesc();
        if (posts.isEmpty()) return "{\"items\":[]}";
        StringBuilder sb = new StringBuilder("{\"items\":[");
        for (int i = 0; i < Math.min(limit, posts.size()); i++) {
            if (i > 0) sb.append(",");
            SocialPost p = posts.get(i);
            long likes = likeRepo.countByPostId(p.getId());
            long comments = commentRepo.countByPostId(p.getId());
            sb.append("{")
              .append("\"id\":").append(p.getId()).append(",")
              .append("\"content\":\"").append(escapeJson(p.getContent())).append("\",")
              .append("\"images\":").append(p.getImages() != null ? p.getImages() : "[]").append(",")
              .append("\"authorName\":\"").append(escapeJson(p.getAuthorName() != null ? p.getAuthorName() : "")).append("\",")
              .append("\"authorImage\":\"").append(escapeJson(p.getAuthorImage() != null ? p.getAuthorImage() : "")).append("\",")
              .append("\"createdAt\":\"").append(p.getCreatedAt() != null ? p.getCreatedAt().toString() : "").append("\",")
              .append("\"likeCount\":").append(likes).append(",")
              .append("\"commentCount\":").append(comments)
              .append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    public String searchToFeedJson(String query, int limit) {
        List<SocialPost> posts = postRepo.search(query);
        if (posts.isEmpty()) return "{\"items\":[]}";
        StringBuilder sb = new StringBuilder("{\"items\":[");
        int count = 0;
        for (SocialPost p : posts) {
            if (count >= limit) break;
            if (count > 0) sb.append(",");
            long likes = likeRepo.countByPostId(p.getId());
            long comments = commentRepo.countByPostId(p.getId());
            sb.append("{")
              .append("\"id\":").append(p.getId()).append(",")
              .append("\"content\":\"").append(escapeJson(p.getContent())).append("\",")
              .append("\"images\":").append(p.getImages() != null ? p.getImages() : "[]").append(",")
              .append("\"authorName\":\"").append(escapeJson(p.getAuthorName() != null ? p.getAuthorName() : "")).append("\",")
              .append("\"authorImage\":\"").append(escapeJson(p.getAuthorImage() != null ? p.getAuthorImage() : "")).append("\",")
              .append("\"createdAt\":\"").append(p.getCreatedAt() != null ? p.getCreatedAt().toString() : "").append("\",")
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
    public SocialComment addComment(Long userId, Long postId, String content) {
        if (!postRepo.existsById(postId)) throw new RuntimeException("Post not found");
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        SocialComment comment = new SocialComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setAuthorName(user.getName());
        comment.setAuthorImage(user.getProfileImage());
        return commentRepo.save(comment);
    }

    public List<SocialComment> getComments(Long postId) {
        return commentRepo.findByPostIdOrderByCreatedAtAsc(postId);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        SocialComment comment = commentRepo.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found"));
        if (!comment.getUserId().equals(userId)) throw new RuntimeException("Unauthorized");
        commentRepo.delete(comment);
    }

    // --- Likes ---

    @Transactional
    public Map<String, Object> toggleLike(Long userId, Long postId) {
        Optional<SocialLike> existing = likeRepo.findByPostIdAndUserId(postId, userId);
        boolean liked;
        if (existing.isPresent()) {
            likeRepo.delete(existing.get());
            liked = false;
        } else {
            SocialLike like = new SocialLike();
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

    // --- Media Upload ---

    public Map<String, String> uploadMedia(Long userId, MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new RuntimeException("File is empty");
        String contentType = file.getContentType();
        if (contentType == null) throw new RuntimeException("Unknown file type");

        boolean isImage = ALLOWED_IMAGE_TYPES.contains(contentType);
        boolean isVideo = ALLOWED_VIDEO_TYPES.contains(contentType);
        if (!isImage && !isVideo) throw new RuntimeException("Unsupported file type");
        if (isImage && file.getSize() > MAX_IMAGE_SIZE) throw new RuntimeException("Image exceeds 5MB limit");
        if (isVideo && file.getSize() > MAX_VIDEO_SIZE) throw new RuntimeException("Video exceeds 50MB limit");

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
        String type = isVideo ? "video" : "image";
        return Map.of("url", url, "type", type);
    }

    // --- Helpers ---

    private Map<String, Object> toFeedItem(SocialPost p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("content", p.getContent());
        m.put("images", p.getImages());
        m.put("authorName", p.getAuthorName());
        m.put("authorImage", p.getAuthorImage());
        m.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        m.put("likeCount", likeRepo.countByPostId(p.getId()));
        m.put("commentCount", commentRepo.countByPostId(p.getId()));
        return m;
    }

    private Map<String, Object> toDetailMap(SocialPost p, long likeCount, long commentCount) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("userId", p.getUserId());
        m.put("content", p.getContent());
        m.put("images", p.getImages());
        m.put("authorName", p.getAuthorName());
        m.put("authorImage", p.getAuthorImage());
        m.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        m.put("likeCount", likeCount);
        m.put("commentCount", commentCount);
        return m;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
