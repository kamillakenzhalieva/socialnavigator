package com.social_navigator_ar.demo.model; // Или ваш выбранный пакет для сущностей

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles") // Это будет новая таблица
public class UserProfiles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true) // добавлено unique
    private Login user;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String diagnosis; // Основной диагноз/потребность пользователя для фильтрации рекомендаций

    private Integer childAge; // Возраст ребенка, если применимо

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Login getUser() { return user; }
    public void setUser(Login user) { this.user = user; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public Integer getChildAge() { return childAge; }
    public void setChildAge(Integer childAge) { this.childAge = childAge; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}