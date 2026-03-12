package com.luxrobo.demoapi.repository;

import com.luxrobo.demoapi.entity.UserClickHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserClickHistoryRepository extends JpaRepository<UserClickHistory, Long> {
    List<UserClickHistory> findTop100ByUserIdOrderByClickedAtDesc(Long userId);
}
