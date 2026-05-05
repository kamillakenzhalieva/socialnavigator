package com.social_navigator_ar.demo.repository;

import com.social_navigator_ar.demo.model.MarkerViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MarkerViewLogRepository extends JpaRepository<MarkerViewLog, Long> {

    @Query("SELECT FUNCTION('DATE', m.viewedAt), COUNT(m) FROM MarkerViewLog m WHERE m.marker.id = :markerId GROUP BY FUNCTION('DATE', m.viewedAt)")
    List<Object[]> countViewsByDate(@Param("markerId") Long markerId);
}