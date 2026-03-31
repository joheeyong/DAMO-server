package com.luxrobo.demoapi.repository;

import com.luxrobo.demoapi.entity.SocialLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialLikeRepository extends JpaRepository<SocialLike, Long> {
    Optional<SocialLike> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
}
