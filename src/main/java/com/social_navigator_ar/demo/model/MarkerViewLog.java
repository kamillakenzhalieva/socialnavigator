package com.social_navigator_ar.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "marker_view_log")
public class MarkerViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "marker_id", nullable = false)
    private Marker marker;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    // Конструкторы

    public MarkerViewLog() {
        this.viewedAt = LocalDateTime.now();
    }

    public MarkerViewLog(Marker marker) {
        this.marker = marker;
        this.viewedAt = LocalDateTime.now();
    }

    // Геттеры и сеттеры

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}