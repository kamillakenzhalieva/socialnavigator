package com.social_navigator_ar.demo.repository;

import com.social_navigator_ar.demo.model.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {
    List<AdminNotification> findTop15ByOrderByCreatedAtDesc();
}
