package com.social_navigator_ar.demo.service;

import com.social_navigator_ar.demo.model.AdminNotification;
import com.social_navigator_ar.demo.repository.AdminNotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminNotificationService {

    private final AdminNotificationRepository repository;

    public AdminNotificationService(AdminNotificationRepository repository) {
        this.repository = repository;
    }

    public void record(String type, String text) {
        AdminNotification notification = new AdminNotification();
        notification.setType(type);
        notification.setText(text);
        notification.setCreatedAt(LocalDateTime.now());
        repository.save(notification);
    }

    public List<AdminNotification> latest() {
        return repository.findTop15ByOrderByCreatedAtDesc();
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) return trimmed;
        return trimmed.substring(0, maxLength) + "…";
    }
}
