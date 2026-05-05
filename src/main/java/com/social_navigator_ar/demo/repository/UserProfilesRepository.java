package com.social_navigator_ar.demo.repository;

import com.social_navigator_ar.demo.model.UserProfiles;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserProfilesRepository extends JpaRepository<UserProfiles, Long> {
    Optional<UserProfiles> findByUserId(Long userId);
}