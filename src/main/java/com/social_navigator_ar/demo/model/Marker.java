package com.social_navigator_ar.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "markers")
public class Marker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название обязательно")
    @Size(max = 255, message = "Название слишком длинное")
    private String name;

    @Size(max = 255, message = "Адрес слишком длинный")
    private String address;

    @NotNull(message = "Широта обязательна")
    @DecimalMin(value = "-90.0", message = "Широта должна быть ≥ -90")
    @DecimalMax(value = "90.0", message = "Широта должна быть ≤ 90")
    private Double latitude;

    @NotNull(message = "Долгота обязательна")
    @DecimalMin(value = "-180.0", message = "Долгота должна быть ≥ -180")
    @DecimalMax(value = "180.0", message = "Долгота должна быть ≤ 180")
    private Double longitude;

    @Size(max = 255, message = "Категория/Диагноз слишком длинный")
    private String diagnosis;

    @Size(max = 20, message = "Некорректный формат возрастной категории")
    private String age;

    @Size(max = 20, message = "Некорректный формат телефона")
    private String phone;

    @Email(message = "Некорректный адрес электронной почты")
    @Size(max = 100, message = "Email слишком длинный")
    private String email;

    private String imageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "marker_images", joinColumns = @JoinColumn(name = "marker_id"))
    @Column(name = "image_url", length = 500)
    @OrderColumn(name = "image_order")
    private List<String> imageUrls = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "marker_image_thumbnails", joinColumns = @JoinColumn(name = "marker_id"))
    @Column(name = "thumbnail_url", length = 500)
    @OrderColumn(name = "image_order")
    private List<String> thumbnailUrls = new ArrayList<>();

    private int views = 0;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    public String getAddress() {return address;}
    public void setAddress(String address) {this.address = address;}
    public Double getLatitude() {return latitude;}
    public void setLatitude(Double latitude) {this.latitude = latitude;}
    public Double getLongitude() {return longitude;}
    public void setLongitude(Double longitude) {this.longitude = longitude;}
    public String getDiagnosis() {return diagnosis;}
    public void setDiagnosis(String diagnosis) {this.diagnosis = diagnosis;}
    public String getAge() {return age;}
    public void setAge(String age) {this.age = age;}
    public String getImageUrl() {return imageUrl;}
    public void setImageUrl(String imageUrl) {this.imageUrl = imageUrl;}
    public List<String> getImageUrls() {return imageUrls;}
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        if (!this.imageUrls.isEmpty()) {
            this.imageUrl = this.imageUrls.get(0);
        }
    }
    public List<String> getThumbnailUrls() {return thumbnailUrls;}
    public void setThumbnailUrls(List<String> thumbnailUrls) {
        this.thumbnailUrls = thumbnailUrls != null ? thumbnailUrls : new ArrayList<>();
    }
    public int getViews() {return views;}
    public void setViews(int views) {this.views = views;}
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Marker() {}

    public Marker(String name, String address, Double latitude, Double longitude, String diagnosis, String age) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.diagnosis = diagnosis;
        this.age = age;
    }

    public Marker(String name, String address, Double latitude, Double longitude, String diagnosis, String age, String imageUrl) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.diagnosis = diagnosis;
        this.age = age;
        this.imageUrl = imageUrl;
    }

    public Marker(String name, String address, Double latitude, Double longitude, String diagnosis, String age, String imageUrl, int views) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.diagnosis = diagnosis;
        this.age = age;
        this.imageUrl = imageUrl;
        this.views = views;
    }
}