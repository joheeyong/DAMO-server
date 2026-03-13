package com.luxrobo.demoapi.repository;

import com.luxrobo.demoapi.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Bookmark> findByUserIdAndContentId(Long userId, String contentId);
    void deleteByUserIdAndContentId(Long userId, String contentId);
    boolean existsByUserIdAndContentId(Long userId, String contentId);
}
