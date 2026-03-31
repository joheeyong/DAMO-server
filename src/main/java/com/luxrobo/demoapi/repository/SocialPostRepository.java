package com.luxrobo.demoapi.repository;

import com.luxrobo.demoapi.entity.SocialPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SocialPostRepository extends JpaRepository<SocialPost, Long> {
    List<SocialPost> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<SocialPost> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<SocialPost> findTop20ByOrderByCreatedAtDesc();

    @Query("SELECT p FROM SocialPost p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%',:query,'%')) ORDER BY p.createdAt DESC")
    List<SocialPost> search(String query);
}
