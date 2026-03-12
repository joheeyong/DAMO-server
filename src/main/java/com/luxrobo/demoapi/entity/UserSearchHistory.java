package com.luxrobo.demoapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_search_history", indexes = {
    @Index(name = "idx_search_user_time", columnList = "userId,searchedAt")
})
public class UserSearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String query;

    private LocalDateTime searchedAt;

    @PrePersist
    protected void onCreate() {
        if (searchedAt == null) searchedAt = LocalDateTime.now();
    }

    public UserSearchHistory() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public LocalDateTime getSearchedAt() { return searchedAt; }
    public void setSearchedAt(LocalDateTime searchedAt) { this.searchedAt = searchedAt; }
}
