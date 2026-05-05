package com.social_navigator_ar.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marker_id", nullable = false)
    private Marker marker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Login user;

    @NotBlank(message = "Текст отзыва не может быть пустым")
    @Size(max = 500, message = "Отзыв не может быть длиннее 250 символов")
    @Column(nullable = false, length = 500)
    private String text;

    @NotNull(message = "ценка обязательна")
    @Min(value = 1, message = "Оценка должна быть не менее 1")
    @Max(value = 5, message = "Оценка должна быть не более 5")
    @Column(nullable = false)
    private Integer rating;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Marker getMarker() { return marker; }
    public void setMarker(Marker marker) { this.marker = marker; }
    public Login getUser() { return user; }
    public void setUser(Login user) { this.user = user; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}