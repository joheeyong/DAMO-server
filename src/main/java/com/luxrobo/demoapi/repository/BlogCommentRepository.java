package com.luxrobo.demoapi.repository;

import com.luxrobo.demoapi.entity.BlogComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BlogCommentRepository extends JpaRepository<BlogComment, Long> {
    List<BlogComment> findByPostIdOrderByCreatedAtAsc(Long postId);
    long countByPostId(Long postId);
}
