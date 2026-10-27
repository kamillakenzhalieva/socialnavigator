package com.social_navigator_ar.demo.controller;

import com.social_navigator_ar.demo.model.Marker;
import com.social_navigator_ar.demo.model.MarkerViewLog;
import com.social_navigator_ar.demo.model.Login;
import com.social_navigator_ar.demo.repository.LoginRepository;
import com.social_navigator_ar.demo.repository.MarkerRepository;
import com.social_navigator_ar.demo.repository.MarkerViewLogRepository;
import com.social_navigator_ar.demo.repository.ReviewRepository;
import com.social_navigator_ar.demo.service.AdminNotificationService;
import com.social_navigator_ar.demo.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/map-points")
public class MapPointController {

    private final MarkerRepository markerRepository;
    private final MarkerViewLogRepository markerViewLogRepository;
    private final FileStorageService fileStorageService;
    private final LoginRepository loginRepository;
    private final AdminNotificationService adminNotificationService;
    private final ReviewRepository reviewRepository;

    public MapPointController(MarkerRepository markerRepository,
                               MarkerViewLogRepository markerViewLogRepository,
                               FileStorageService fileStorageService,
                               LoginRepository loginRepository,
                               AdminNotificationService adminNotificationService,
                               ReviewRepository reviewRepository) {
        this.markerRepository = markerRepository;
        this.markerViewLogRepository = markerViewLogRepository;
        this.fileStorageService = fileStorageService;
        this.loginRepository = loginRepository;
        this.adminNotificationService = adminNotificationService;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping
    public List<Marker> getMapPoints() {
        return markerRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Marker> getMapPoint(@PathVariable Long id) {
        return markerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createMarker(
            @RequestParam String name,
            @RequestParam(required = false) String address,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) String diagnosis,
            @RequestParam(required = false) String age,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
            @RequestParam(value = "thumbnails", required = false) List<MultipartFile> thumbnails,
            Authentication authentication
    ) {
        Marker marker = new Marker();
        marker.setName(name);
        marker.setAddress(address);
        marker.setLatitude(latitude);
        marker.setLongitude(longitude);
        marker.setDiagnosis(diagnosis);
        marker.setAge(age);
        marker.setPhone(phone);
        marker.setEmail(email);

        List<String> urls = new ArrayList<>();
        List<String> thumbUrls = new ArrayList<>();
        try {
            if (photos != null) {
                for (int i = 0; i < photos.size(); i++) {
                    MultipartFile photo = photos.get(i);
                    if (photo != null && !photo.isEmpty()) {
                        String fullUrl = fileStorageService.storeFile(photo);
                        urls.add(fullUrl);

                        MultipartFile thumb = (thumbnails != null && i < thumbnails.size()) ? thumbnails.get(i) : null;
                        thumbUrls.add((thumb != null && !thumb.isEmpty()) ? fileStorageService.storeFile(thumb) : fullUrl);
                    }
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Ошибка при загрузке фото"));
        }
        marker.setImageUrls(urls);
        marker.setThumbnailUrls(thumbUrls);

        Marker saved = markerRepository.save(marker);

        if (authentication != null) {
            String adminName = loginRepository.findByEmail(authentication.getName())
                    .map(Login::getName)
                    .orElse(authentication.getName());
            adminNotificationService.record(
                    "MARKER_ADDED",
                    "Добавлена новая организация «" + saved.getName() + "» администратором " + adminName
            );
        }

        return ResponseEntity.ok(saved);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateMarker(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String address,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) String diagnosis,
            @RequestParam(required = false) String age,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(value = "existingImages", required = false) List<String> existingImages,
            @RequestParam(value = "existingThumbnails", required = false) List<String> existingThumbnails,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
            @RequestParam(value = "thumbnails", required = false) List<MultipartFile> thumbnails,
            Authentication authentication
    ) {
        Optional<Marker> optionalMarker = markerRepository.findById(id);
        if (optionalMarker.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Marker marker = optionalMarker.get();
        marker.setName(name);
        marker.setAddress(address);
        marker.setLatitude(latitude);
        marker.setLongitude(longitude);
        marker.setDiagnosis(diagnosis);
        marker.setAge(age);
        marker.setPhone(phone);
        marker.setEmail(email);

        List<String> urls = new ArrayList<>();
        List<String> thumbUrls = new ArrayList<>();
        if (existingImages != null) {
            for (int i = 0; i < existingImages.size(); i++) {
                String fullUrl = existingImages.get(i);
                urls.add(fullUrl);
                String existingThumb = (existingThumbnails != null && i < existingThumbnails.size())
                        ? existingThumbnails.get(i) : null;
                thumbUrls.add((existingThumb != null && !existingThumb.isBlank()) ? existingThumb : fullUrl);
            }
        }
        try {
            if (photos != null) {
                for (int i = 0; i < photos.size(); i++) {
                    MultipartFile photo = photos.get(i);
                    if (photo != null && !photo.isEmpty()) {
                        String fullUrl = fileStorageService.storeFile(photo);
                        urls.add(fullUrl);

                        MultipartFile thumb = (thumbnails != null && i < thumbnails.size()) ? thumbnails.get(i) : null;
                        thumbUrls.add((thumb != null && !thumb.isEmpty()) ? fileStorageService.storeFile(thumb) : fullUrl);
                    }
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Ошибка при загрузке фото"));
        }
        marker.setImageUrls(urls);
        marker.setThumbnailUrls(thumbUrls);
        if (urls.isEmpty()) {
            marker.setImageUrl(null);
        }

        Marker saved = markerRepository.save(marker);

        if (authentication != null) {
            String adminName = loginRepository.findByEmail(authentication.getName())
                    .map(Login::getName)
                    .orElse(authentication.getName());
            adminNotificationService.record(
                    "MARKER_UPDATED",
                    "Изменены данные организации «" + saved.getName() + "» администратором " + adminName
            );
        }

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> deleteMarker(@PathVariable Long id, Authentication authentication) {
        Optional<Marker> optionalMarker = markerRepository.findById(id);
        if (optionalMarker.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String markerName = optionalMarker.get().getName();

        reviewRepository.deleteByMarkerId(id);
        markerViewLogRepository.deleteByMarkerId(id);
        markerRepository.deleteById(id);

        if (authentication != null) {
            String adminName = loginRepository.findByEmail(authentication.getName())
                    .map(Login::getName)
                    .orElse(authentication.getName());
            adminNotificationService.record(
                    "MARKER_DELETED",
                    "Удалена организация «" + markerName + "» администратором " + adminName
            );
        }

        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((org.springframework.validation.FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

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
        markerViewLogRepository.save(log);

        return ResponseEntity.ok(marker);
    }

    @GetMapping("/{id}/view-stats")
    public ResponseEntity<List<Map<String, Object>>> getViewStats(@PathVariable Long id) {
        List<Object[]> stats = markerViewLogRepository.countViewsByDate(id);
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