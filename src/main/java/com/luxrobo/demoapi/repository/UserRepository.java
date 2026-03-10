package com.luxrobo.demoapi.repository;

import com.luxrobo.demoapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
