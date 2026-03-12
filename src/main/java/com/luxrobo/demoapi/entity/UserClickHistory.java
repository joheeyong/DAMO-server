package com.luxrobo.demoapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_click_history", indexes = {
    @Index(name = "idx_click_user_time", columnList = "userId,clickedAt")
})
public class UserClickHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String contentId;

    @Column(nullable = false)
    private String platform;

    private String sourceKeyword;

    private LocalDateTime clickedAt;

    @PrePersist
    protected void onCreate() {
        if (clickedAt == null) clickedAt = LocalDateTime.now();
    }

    public UserClickHistory() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getSourceKeyword() { return sourceKeyword; }
    public void setSourceKeyword(String sourceKeyword) { this.sourceKeyword = sourceKeyword; }
    public LocalDateTime getClickedAt() { return clickedAt; }
    public void setClickedAt(LocalDateTime clickedAt) { this.clickedAt = clickedAt; }
}
