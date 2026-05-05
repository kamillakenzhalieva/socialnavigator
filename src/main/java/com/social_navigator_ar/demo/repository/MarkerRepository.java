package com.social_navigator_ar.demo.repository;

import com.social_navigator_ar.demo.model.Marker; // Используйте вашу сущность Marker
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
// import java.util.List; // JpaRepository уже предоставляет findAll()

@Repository // Опционально, JpaRepository уже является Spring бином
public interface MarkerRepository extends JpaRepository<Marker, Long> {
    // findAll() наследуется
    // Здесь вы можете добавить пользовательские методы запросов, если необходимо
}