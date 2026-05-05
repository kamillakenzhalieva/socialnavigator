package com.social_navigator_ar.demo.repository;

import com.social_navigator_ar.demo.model.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoginRepository extends JpaRepository<Login, Long> {
    Optional<Login> findByEmail(String email);
    boolean existsByEmail(String email);
    // List<Login> findByIsAdminTrue(); // Если вам нужно найти всех администраторов
}