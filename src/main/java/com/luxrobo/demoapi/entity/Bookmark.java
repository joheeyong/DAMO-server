package com.luxrobo.demoapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookmarks", indexes = {
    @Index(name = "idx_bookmark_user", columnList = "userId,createdAt")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_bookmark_user_content", columnNames = {"userId", "contentId"})
})
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String contentId;

    @Column(nullable = false)
    private String platform;

    @Column(length = 500)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String link;

    @Column(length = 1000)
    private String image;

    private String author;

    private String date;

    @Column(columnDefinition = "TEXT")
    private String extraJson;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Bookmark() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getExtraJson() { return extraJson; }
    public void setExtraJson(String extraJson) { this.extraJson = extraJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
