package com.social_navigator_ar.demo.repository;

import com.social_navigator_ar.demo.model.ImportRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImportRunRepository extends JpaRepository<ImportRun, Long> {
    Optional<ImportRun> findFirstByOrderByStartedAtDesc();
}
