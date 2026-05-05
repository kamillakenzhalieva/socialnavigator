package com.social_navigator_ar.demo.controller;

import com.social_navigator_ar.demo.model.Marker;
import com.social_navigator_ar.demo.model.MarkerViewLog;
import com.social_navigator_ar.demo.repository.MarkerRepository;
import com.social_navigator_ar.demo.repository.MarkerViewLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/map-points")
public class MapPointController {

    private final MarkerRepository markerRepository;
    private final MarkerViewLogRepository markerViewLogRepository;

    public MapPointController(MarkerRepository markerRepository, MarkerViewLogRepository markerViewLogRepository) {
        this.markerRepository = markerRepository;
        this.markerViewLogRepository = markerViewLogRepository;
    }

    // Получение всех маркеров
    @GetMapping
    public List<Marker> getMapPoints() {
        return markerRepository.findAll();
    }

    // Увеличение количества просмотров
    @PostMapping("/{id}/increment-view")
    public ResponseEntity<Marker> incrementView(@PathVariable Long id) {
        Optional<Marker> optionalMarker = markerRepository.findById(id);
        if (optionalMarker.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Marker marker = optionalMarker.get();
        marker.setViews(marker.getViews() + 1);
        markerRepository.save(marker);

        MarkerViewLog log = new MarkerViewLog(marker);
        markerViewLogRepository.save(log); // Используем экземпляр репозитория

        return ResponseEntity.ok(marker);
    }

    @GetMapping("/{id}/view-stats")
    public ResponseEntity<List<Map<String, Object>>> getViewStats(@PathVariable Long id) {
        List<Object[]> stats = markerViewLogRepository.countViewsByDate(id); // Используем экземпляр репозитория
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : stats) {
            Map<String, Object> map = new HashMap<>();
            map.put("date", row[0]);
            map.put("count", row[1]);
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }
}