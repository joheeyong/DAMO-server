package com.luxrobo.demoapi.repository;

import com.luxrobo.demoapi.entity.SocialComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SocialCommentRepository extends JpaRepository<SocialComment, Long> {
    List<SocialComment> findByPostIdOrderByCreatedAtAsc(Long postId);
    long countByPostId(Long postId);
}
