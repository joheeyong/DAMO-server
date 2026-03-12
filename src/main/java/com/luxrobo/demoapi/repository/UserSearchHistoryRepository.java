package com.luxrobo.demoapi.repository;

import com.luxrobo.demoapi.entity.UserSearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserSearchHistoryRepository extends JpaRepository<UserSearchHistory, Long> {
    List<UserSearchHistory> findTop50ByUserIdOrderBySearchedAtDesc(Long userId);
}
