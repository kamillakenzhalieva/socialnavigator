package com.social_navigator_ar.demo.repository;

import com.social_navigator_ar.demo.model.Marker; // Используйте вашу сущность Marker
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarkerRepository extends JpaRepository<Marker, Long> {
}