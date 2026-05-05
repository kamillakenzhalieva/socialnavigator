package com.social_navigator_ar.demo.controller;

import com.social_navigator_ar.demo.dto.ReviewDTO;
import com.social_navigator_ar.demo.model.Login;
import com.social_navigator_ar.demo.model.Marker;
import com.social_navigator_ar.demo.model.Review;
import com.social_navigator_ar.demo.repository.LoginRepository;
import com.social_navigator_ar.demo.repository.MarkerRepository;
import com.social_navigator_ar.demo.repository.ReviewRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final MarkerRepository markerRepository;
    private final LoginRepository loginRepository;

    public ReviewController(
            ReviewRepository reviewRepository,
            MarkerRepository markerRepository,
            LoginRepository loginRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.markerRepository = markerRepository;
        this.loginRepository = loginRepository;
    }

    /**
     * Получает список отзывов для маркера в формате DTO
     */
    @GetMapping("/marker/{markerId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByMarker(@PathVariable Long markerId) {
        try {
            System.out.println("Запрос на получение отзывов для маркера ID: " + markerId);
            List<ReviewDTO> dtoList = reviewRepository.findDTOsByMarkerId(markerId);
            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            System.err.println("Ошибка получения отзывов: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Добавляет новый отзыв
     */
    @PostMapping
    public ResponseEntity<ReviewDTO> addReview(
            @Valid @RequestBody Review review,
            BindingResult result,
            Authentication authentication
    ) {
        if (result.hasErrors()) {
            throw new IllegalArgumentException("Validation failed");
        }

        String email = authentication.getName();

        Login user = loginRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Marker marker = markerRepository.findById(review.getMarker().getId())
                .orElseThrow(() -> new RuntimeException("Marker not found"));

        review.setUser(user);
        review.setMarker(marker);
        review.setCreatedAt(LocalDateTime.now());

        // Сохраняем отзыв
        Review savedReview = reviewRepository.save(review);

        // Явно получаем данные, избегая ленивой загрузки
        Long markerId = marker.getId();  // Уже загружен
        String userName = user.getName(); // Уже загружен

        // Создаём DTO без вызова getMarker() / getUser()
        ReviewDTO dto = new ReviewDTO(
                savedReview.getId(),
                savedReview.getText(),
                savedReview.getRating(),
                savedReview.getCreatedAt(),
                userName,
                markerId
        );

        return ResponseEntity.ok(dto);
    }
}