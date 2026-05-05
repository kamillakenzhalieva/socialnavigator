package com.social_navigator_ar.demo.repository;

import com.social_navigator_ar.demo.dto.ReviewDTO;
import com.social_navigator_ar.demo.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT new com.social_navigator_ar.demo.dto.ReviewDTO(r.id, r.text, r.rating, r.createdAt, u.name, m.id) " +
            "FROM Review r " +
            "JOIN r.marker m " +
            "JOIN r.user u " +
            "WHERE m.id = :markerId " +
            "ORDER BY r.createdAt ASC")
    List<ReviewDTO> findDTOsByMarkerId(@Param("markerId") Long markerId);

    //List<Review> findByMarkerIdOrderByCreatedAtDesc(Long );
}