package com.social_navigator_ar.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "is_admin_response", nullable = false)
    private boolean isAdminResponse;

    @Column(name = "parent_message_id")
    private Long parentMessageId;

    // Конструкторы
    public Message() {
        this.isAdminResponse = false; // По умолчанию сообщение не от админа
    }

    public Message(String userId, String text, LocalDateTime timestamp) {
        this.userId = userId;
        this.text = text;
        this.timestamp = timestamp;
        this.isAdminResponse = false;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isAdminResponse() { return isAdminResponse; } // Переименован
    public void setIsAdminResponse(boolean isAdminResponse) { this.isAdminResponse = isAdminResponse; }

    public Long getParentMessageId() { return parentMessageId; }
    public void setParentMessageId(Long parentMessageId) { this.parentMessageId = parentMessageId; }
}