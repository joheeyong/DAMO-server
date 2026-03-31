package com.luxrobo.demoapi.service;

import com.luxrobo.demoapi.entity.*;
import com.luxrobo.demoapi.repository.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final UserRepository userRepository;
    private final UserSearchHistoryRepository searchHistoryRepository;
    private final UserClickHistoryRepository clickHistoryRepository;
    private final BlogPostRepository blogPostRepository;
    private final BlogLikeRepository blogLikeRepository;
    private final BlogCommentRepository blogCommentRepository;
    private final SocialPostRepository socialPostRepository;
    private final SocialLikeRepository socialLikeRepository;
    private final SocialCommentRepository socialCommentRepository;

    public RecommendationService(UserRepository userRepository,
                                  UserSearchHistoryRepository searchHistoryRepository,
                                  UserClickHistoryRepository clickHistoryRepository,
                                  BlogPostRepository blogPostRepository,
                                  BlogLikeRepository blogLikeRepository,
                                  BlogCommentRepository blogCommentRepository,
                                  SocialPostRepository socialPostRepository,
                                  SocialLikeRepository socialLikeRepository,
                                  SocialCommentRepository socialCommentRepository) {
        this.userRepository = userRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.clickHistoryRepository = clickHistoryRepository;
        this.blogPostRepository = blogPostRepository;
        this.blogLikeRepository = blogLikeRepository;
        this.blogCommentRepository = blogCommentRepository;
        this.socialPostRepository = socialPostRepository;
        this.socialLikeRepository = socialLikeRepository;
        this.socialCommentRepository = socialCommentRepository;
    }

    /**
     * Rank content items based on user profile, search history, and click history.
     * Returns ordered list of content IDs (highest score first).
     */
    public List<String> rankItems(Long userId, List<Map<String, String>> items) {
        User user = userRepository.findById(userId).orElse(null);
        List<UserSearchHistory> recentSearches = searchHistoryRepository.findTop50ByUserIdOrderBySearchedAtDesc(userId);
        List<UserClickHistory> recentClicks = clickHistoryRepository.findTop100ByUserIdOrderByClickedAtDesc(userId);

        // Build user profile
        List<String> interests = new ArrayList<>();
        if (user != null && user.getInterests() != null && !user.getInterests().isEmpty()) {
            interests = Arrays.stream(user.getInterests().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        Set<String> clickedContentIds = recentClicks.stream()
                .map(UserClickHistory::getContentId)
                .collect(Collectors.toSet());

        // Platform click counts with decay
        Map<String, Double> platformAffinity = new HashMap<>();
        for (UserClickHistory click : recentClicks) {
            double d = decay(click.getClickedAt());
            platformAffinity.merge(click.getPlatform(), d, Double::sum);
        }

        // Keyword affinity from clicks
        Map<String, Double> keywordAffinity = new HashMap<>();
        for (UserClickHistory click : recentClicks) {
            if (click.getSourceKeyword() != null && !click.getSourceKeyword().isEmpty()) {
                double d = decay(click.getClickedAt());
                keywordAffinity.merge(click.getSourceKeyword(), d, Double::sum);
            }
        }

        // Search keyword affinity
        Map<String, Double> searchAffinity = new HashMap<>();
        for (UserSearchHistory search : recentSearches) {
            double d = decay(search.getSearchedAt());
            searchAffinity.merge(search.getQuery().toLowerCase(), d, Double::sum);
        }

        LocalDateTime now = LocalDateTime.now();
        Random random = new Random();

        // Score each item
        List<ScoredItem> scored = new ArrayList<>();
        for (Map<String, String> item : items) {
            String id = item.getOrDefault("id", "");
            String platform = item.getOrDefault("platform", "");
            String title = item.getOrDefault("title", "").toLowerCase();
            String sourceKeyword = item.getOrDefault("sourceKeyword", "");

            double score = 0.0;

            // 1. Interest match (+10 per match)
            for (String interest : interests) {
                String lowerInterest = interest.toLowerCase();
                if (sourceKeyword.toLowerCase().contains(lowerInterest) || title.contains(lowerInterest)) {
                    score += 10.0;
                }
            }

            // 2. Search history match (+5 * decay)
            for (Map.Entry<String, Double> entry : searchAffinity.entrySet()) {
                String searchQuery = entry.getKey();
                if (!sourceKeyword.isEmpty() && sourceKeyword.toLowerCase().contains(searchQuery)) {
                    score += 5.0 * Math.min(entry.getValue(), 3.0);
                } else if (title.contains(searchQuery)) {
                    score += 3.0 * Math.min(entry.getValue(), 3.0);
                }
            }

            // 3. Platform affinity (+0.3 * decayed count, capped)
            Double platformScore = platformAffinity.get(platform);
            if (platformScore != null) {
                score += Math.min(platformScore, 10.0) * 0.3;
            }

            // 4. Keyword affinity from clicks (+7 * decay)
            if (!sourceKeyword.isEmpty()) {
                Double kwScore = keywordAffinity.get(sourceKeyword);
                if (kwScore != null) {
                    score += 7.0 * Math.min(kwScore, 3.0);
                }
            }

            // 5. Already-clicked penalty
            if (clickedContentIds.contains(id)) {
                score -= 15.0;
            }

            // 6. Small random jitter for variety
            score += random.nextDouble() * 0.5;

            scored.add(new ScoredItem(id, score));
        }

        // Sort by score descending
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        return scored.stream().map(s -> s.id).collect(Collectors.toList());
    }

    /**
     * Generate personalized recommendations based on user activity.
     * Analyzes clicks, searches, and interests to recommend:
     * 1. DAMO blog/feed posts matching user patterns
     * 2. Suggested search keywords
     * Returns items with reasons ("why" the recommendation was made).
     */
    public Map<String, Object> getRecommendations(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return Map.of("items", List.of(), "keywords", List.of());

        List<UserClickHistory> clicks = clickHistoryRepository.findTop100ByUserIdOrderByClickedAtDesc(userId);
        List<UserSearchHistory> searches = searchHistoryRepository.findTop50ByUserIdOrderBySearchedAtDesc(userId);

        // --- 1. Build user profile ---
        List<String> interests = new ArrayList<>();
        if (user.getInterests() != null && !user.getInterests().isEmpty()) {
            interests = Arrays.stream(user.getInterests().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        }

        // Top keywords from clicks (weighted by decay)
        Map<String, Double> keywordScores = new LinkedHashMap<>();
        for (UserClickHistory c : clicks) {
            if (c.getSourceKeyword() != null && !c.getSourceKeyword().isEmpty()) {
                keywordScores.merge(c.getSourceKeyword(), decay(c.getClickedAt()), Double::sum);
            }
        }

        // Top keywords from searches (weighted by decay)
        for (UserSearchHistory s : searches) {
            if (s.getQuery() != null && !s.getQuery().trim().isEmpty()) {
                keywordScores.merge(s.getQuery().trim(), decay(s.getSearchedAt()) * 0.8, Double::sum);
            }
        }

        // Add interests with base score
        for (String interest : interests) {
            keywordScores.merge(interest, 2.0, Double::sum);
        }

        // Top platforms from clicks
        Map<String, Double> platformScores = new LinkedHashMap<>();
        for (UserClickHistory c : clicks) {
            platformScores.merge(c.getPlatform(), decay(c.getClickedAt()), Double::sum);
        }

        // Sort keywords by score
        List<Map.Entry<String, Double>> topKeywords = keywordScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        // Sort platforms by score
        List<Map.Entry<String, Double>> topPlatforms = platformScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        // Already clicked DAMO content IDs
        Set<String> clickedDamoIds = clicks.stream()
                .filter(c -> "damo-blog".equals(c.getPlatform()) || "damo-feed".equals(c.getPlatform()))
                .map(UserClickHistory::getContentId)
                .collect(Collectors.toSet());

        // --- 2. Find matching DAMO content ---
        List<Map<String, Object>> recommendations = new ArrayList<>();
        Set<Long> usedBlogIds = new HashSet<>();
        Set<Long> usedSocialIds = new HashSet<>();

        // Search DAMO blog posts by top keywords
        for (Map.Entry<String, Double> kw : topKeywords) {
            if (recommendations.size() >= 10) break;
            List<BlogPost> blogMatches = blogPostRepository.searchPublished(kw.getKey());
            for (BlogPost p : blogMatches) {
                if (usedBlogIds.contains(p.getId())) continue;
                if (clickedDamoIds.contains("damo-blog-" + p.getId())) continue;
                usedBlogIds.add(p.getId());
                long likes = blogLikeRepository.countByPostId(p.getId());
                long comments = blogCommentRepository.countByPostId(p.getId());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", p.getId());
                item.put("type", "damo-blog");
                item.put("title", p.getTitle());
                item.put("description", p.getSummary() != null ? p.getSummary() : "");
                item.put("image", p.getCoverImage() != null ? p.getCoverImage() : "");
                item.put("author", p.getAuthorName() != null ? p.getAuthorName() : "");
                item.put("date", p.getPublishedAt() != null ? p.getPublishedAt().toString().substring(0, 10) : "");
                item.put("likeCount", likes);
                item.put("commentCount", comments);
                item.put("reason", buildReason(kw.getKey(), "damo-blog", interests, searches));
                item.put("score", kw.getValue());
                recommendations.add(item);
                if (recommendations.size() >= 10) break;
            }
        }

        // Search DAMO social posts by top keywords
        for (Map.Entry<String, Double> kw : topKeywords) {
            if (recommendations.size() >= 15) break;
            List<SocialPost> socialMatches = socialPostRepository.search(kw.getKey());
            for (SocialPost p : socialMatches) {
                if (usedSocialIds.contains(p.getId())) continue;
                if (clickedDamoIds.contains("damo-feed-" + p.getId())) continue;
                usedSocialIds.add(p.getId());
                long likes = socialLikeRepository.countByPostId(p.getId());
                long comments = socialCommentRepository.countByPostId(p.getId());
                String firstImage = "";
                if (p.getImages() != null && p.getImages().contains("\"url\"")) {
                    try {
                        int urlIdx = p.getImages().indexOf("\"url\"");
                        int start = p.getImages().indexOf("\"", urlIdx + 5) + 1;
                        int end = p.getImages().indexOf("\"", start);
                        if (start > 0 && end > start) firstImage = p.getImages().substring(start, end);
                    } catch (Exception ignored) {}
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", p.getId());
                item.put("type", "damo-feed");
                item.put("title", p.getContent() != null ? p.getContent().substring(0, Math.min(80, p.getContent().length())) : "");
                item.put("description", p.getContent() != null ? p.getContent().substring(0, Math.min(200, p.getContent().length())) : "");
                item.put("image", firstImage);
                item.put("author", p.getAuthorName() != null ? p.getAuthorName() : "");
                item.put("date", p.getCreatedAt() != null ? p.getCreatedAt().toString().substring(0, 10) : "");
                item.put("likeCount", likes);
                item.put("commentCount", comments);
                item.put("reason", buildReason(kw.getKey(), "damo-feed", interests, searches));
                item.put("score", kw.getValue());
                recommendations.add(item);
                if (recommendations.size() >= 15) break;
            }
        }

        // Sort recommendations by score descending
        recommendations.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));

        // --- 3. Suggest search keywords ---
        List<Map<String, Object>> suggestedKeywords = new ArrayList<>();
        for (Map.Entry<String, Double> kw : topKeywords) {
            if (suggestedKeywords.size() >= 5) break;
            String keyword = kw.getKey();
            String reason;
            if (interests.stream().anyMatch(i -> i.equalsIgnoreCase(keyword))) {
                reason = "관심사";
            } else {
                long searchCount = searches.stream().filter(s -> s.getQuery().equalsIgnoreCase(keyword)).count();
                long clickCount = clicks.stream().filter(c -> keyword.equals(c.getSourceKeyword())).count();
                if (searchCount > 0 && clickCount > 0) {
                    reason = "자주 검색하고 클릭해요";
                } else if (searchCount > 0) {
                    reason = "자주 검색해요";
                } else {
                    reason = "관련 콘텐츠를 자주 봐요";
                }
            }
            suggestedKeywords.add(Map.of("keyword", keyword, "reason", reason, "score", kw.getValue()));
        }

        // --- 4. Top platforms info ---
        List<Map<String, Object>> favPlatforms = new ArrayList<>();
        for (Map.Entry<String, Double> p : topPlatforms) {
            if (favPlatforms.size() >= 3) break;
            favPlatforms.add(Map.of("platform", p.getKey(), "score", p.getValue()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", recommendations);
        result.put("keywords", suggestedKeywords);
        result.put("platforms", favPlatforms);
        result.put("userName", user.getName() != null ? user.getName() : "");
        return result;
    }

    private String buildReason(String keyword, String type, List<String> interests, List<UserSearchHistory> searches) {
        boolean isInterest = interests.stream().anyMatch(i -> i.equalsIgnoreCase(keyword));
        boolean isSearched = searches.stream().anyMatch(s -> s.getQuery().equalsIgnoreCase(keyword));

        if (isInterest) {
            return "'" + keyword + "' 관심사와 관련있어요";
        } else if (isSearched) {
            return "'" + keyword + "'을(를) 검색하셨죠";
        } else {
            return "'" + keyword + "' 관련 콘텐츠를 자주 보셨어요";
        }
    }

    /**
     * Time decay: recent actions matter more.
     * Half-life of 72 hours.
     */
    private double decay(LocalDateTime timestamp) {
        if (timestamp == null) return 0.5;
        long hoursAgo = Duration.between(timestamp, LocalDateTime.now()).toHours();
        return 1.0 / (1.0 + hoursAgo / 72.0);
    }

    private static class ScoredItem {
        final String id;
        final double score;

        ScoredItem(String id, double score) {
            this.id = id;
            this.score = score;
        }
    }
}
