package com.social_navigator_ar.demo.model; // Или ваш выбранный пакет для сущностей

import jakarta.persistence.*;

@Entity
@Table(name = "users") // Убедитесь, что это соответствует вашей существующей таблице users
public class Login {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "is_admin", nullable = false, columnDefinition = "boolean default false") // Убедитесь, что имя столбца соответствует БД, если он существует
    private boolean isAdmin = false;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
}