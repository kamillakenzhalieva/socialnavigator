package com.social_navigator_ar.demo.dto;

import java.time.LocalDateTime;

public class ReviewDTO {

    private Long id;
    private Long markerId;
    private String userName;
    private String text;
    private Integer rating;
    private LocalDateTime createdAt;

    // Конструктор для JPQL (важен порядок!)
    public ReviewDTO(
            Long id,
            String text,
            Integer rating,
            LocalDateTime createdAt,
            String userName,
            Long markerId
    ) {
        this.id = id;
        this.text = text;
        this.rating = rating;
        this.createdAt = createdAt;
        this.userName = userName;
        this.markerId = markerId;
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMarkerId() {
        return markerId;
    }

    public void setMarkerId(Long markerId) {
        this.markerId = markerId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}