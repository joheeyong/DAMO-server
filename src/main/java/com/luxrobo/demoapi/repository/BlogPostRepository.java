package com.luxrobo.demoapi.repository;

import com.luxrobo.demoapi.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    List<BlogPost> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<BlogPost> findByStatusOrderByPublishedAtDesc(String status, Pageable pageable);
    List<BlogPost> findTop10ByStatusOrderByPublishedAtDesc(String status);

    @Query("SELECT p FROM BlogPost p WHERE p.status = 'PUBLISHED' AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.summary) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.tags) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<BlogPost> searchPublished(String q);

    @Modifying
    @Query("UPDATE BlogPost p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(Long id);
}
