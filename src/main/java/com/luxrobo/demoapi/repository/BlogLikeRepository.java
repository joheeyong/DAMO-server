package com.luxrobo.demoapi.repository;

import com.luxrobo.demoapi.entity.BlogLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BlogLikeRepository extends JpaRepository<BlogLike, Long> {
    Optional<BlogLike> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
}
