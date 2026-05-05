package com.social_navigator_ar.demo.service;

import com.social_navigator_ar.demo.dto.ReviewDTO;
import com.social_navigator_ar.demo.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public List<ReviewDTO> getReviewsByMarkerId(Long markerId) {
        return reviewRepository.findDTOsByMarkerId(markerId);
    }
}