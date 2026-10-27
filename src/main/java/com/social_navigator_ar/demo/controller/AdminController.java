package com.social_navigator_ar.demo.controller;

import com.social_navigator_ar.demo.model.Login;
import com.social_navigator_ar.demo.model.Marker;
import com.social_navigator_ar.demo.model.Message;
import com.social_navigator_ar.demo.repository.LoginRepository;
import com.social_navigator_ar.demo.repository.MarkerRepository;
import com.social_navigator_ar.demo.repository.MarkerViewLogRepository;
import com.social_navigator_ar.demo.repository.MessageRepository;
import com.social_navigator_ar.demo.repository.ReviewRepository;
import com.social_navigator_ar.demo.service.AdminNotificationService;
import com.social_navigator_ar.demo.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final LoginRepository loginRepository;
    private final MarkerRepository markerRepository;
    private final MessageRepository messageRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final AdminNotificationService adminNotificationService;
    private final ReviewRepository reviewRepository;
    private final MarkerViewLogRepository markerViewLogRepository;

    public AdminController(LoginRepository loginRepository,
                           MarkerRepository markerRepository,
                           MessageRepository messageRepository,
                           FileStorageService fileStorageService,
                           PasswordEncoder passwordEncoder,
                           AdminNotificationService adminNotificationService,
                           ReviewRepository reviewRepository,
                           MarkerViewLogRepository markerViewLogRepository) {
        this.loginRepository = loginRepository;
        this.markerRepository = markerRepository;
        this.messageRepository = messageRepository;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = passwordEncoder;
        this.adminNotificationService = adminNotificationService;
        this.reviewRepository = reviewRepository;
        this.markerViewLogRepository = markerViewLogRepository;
    }

    @GetMapping("/panel")
    public String adminPanel(Model model, Authentication authentication) {
        String email = authentication.getName();
        Login adminUser = loginRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Администратор не найден: " + email));

        model.addAttribute("adminUser", adminUser);
        model.addAttribute("isSuperAdmin", adminUser.isSuperAdmin());
        model.addAttribute("notifications", adminNotificationService.latest());

        return "admin-panel";
    }

    @GetMapping("/organizations")
    public String organizationsPage(Model model, Authentication authentication) {
        String email = authentication.getName();
        Login adminUser = loginRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Администратор не найден: " + email));

        List<Marker> markers = markerRepository.findAll();

        model.addAttribute("adminUser", adminUser);
        model.addAttribute("isSuperAdmin", adminUser.isSuperAdmin());
        model.addAttribute("markers", markers);

        if (!model.containsAttribute("newMarker")) {
            model.addAttribute("newMarker", new Marker());
        }
        if (!model.containsAttribute("editMarker")) {
            model.addAttribute("editMarker", new Marker());
        }

        return "admin-organizations";
    }

    @PostMapping("/markers/add")
    public String addMarker(@Valid @ModelAttribute("newMarker") Marker marker,
                            BindingResult result,
                            @RequestParam("photo") MultipartFile photo,
                            RedirectAttributes redirectAttributes,
                            Authentication authentication) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.newMarker", result);
            redirectAttributes.addFlashAttribute("newMarker", marker);
            return "redirect:/admin/organizations";
        }

        try {
            if (!photo.isEmpty()) {
                String imageUrl = fileStorageService.storeFile(photo);
                marker.setImageUrls(List.of(imageUrl));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при загрузке фото");
            return "redirect:/admin/organizations";
        }

        markerRepository.save(marker);

        String adminName = loginRepository.findByEmail(authentication.getName())
                .map(Login::getName)
                .orElse(authentication.getName());
        adminNotificationService.record(
                "MARKER_ADDED",
                "Добавлена новая организация «" + marker.getName() + "» администратором " + adminName
        );

        redirectAttributes.addFlashAttribute("successMessage", "Маркер успешно добавлен");
        return "redirect:/admin/organizations";
    }

    @PostMapping("/markers/update")
    public String updateMarker(@Valid @ModelAttribute("editMarker") Marker marker,
                               BindingResult result,
                               @RequestParam(name = "photo", required = false) MultipartFile photo,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.editMarker", result);
            redirectAttributes.addFlashAttribute("editMarker", marker);
            redirectAttributes.addFlashAttribute("openEditModalForId", marker.getId());
            return "redirect:/admin/organizations";
        }

        Marker existingMarker = markerRepository.findById(marker.getId())
                .orElseThrow(() -> new IllegalArgumentException("Неверный ID маркера"));

        try {
            List<String> images = new java.util.ArrayList<>(existingMarker.getImageUrls());
            if (photo != null && !photo.isEmpty()) {
                String imageUrl = fileStorageService.storeFile(photo);
                images.add(imageUrl);
            }
            marker.setImageUrls(images);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при загрузке фото");
            return "redirect:/admin/organizations";
        }

        markerRepository.save(marker);

        String adminName = loginRepository.findByEmail(authentication.getName())
                .map(Login::getName)
                .orElse(authentication.getName());
        adminNotificationService.record(
                "MARKER_UPDATED",
                "Изменены данные организации «" + marker.getName() + "» администратором " + adminName
        );

        redirectAttributes.addFlashAttribute("successMessage", "Маркер успешно обновлён");
        return "redirect:/admin/organizations";
    }

    @PostMapping("/markers/delete")
    @org.springframework.transaction.annotation.Transactional
    public String deleteMarker(@RequestParam Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        Optional<Marker> markerOpt = markerRepository.findById(id);
        if (markerOpt.isPresent()) {
            String markerName = markerOpt.get().getName();

            reviewRepository.deleteByMarkerId(id);
            markerViewLogRepository.deleteByMarkerId(id);
            markerRepository.deleteById(id);

            String adminName = loginRepository.findByEmail(authentication.getName())
                    .map(Login::getName)
                    .orElse(authentication.getName());
            adminNotificationService.record(
                    "MARKER_DELETED",
                    "Удалена организация «" + markerName + "» администратором " + adminName
            );

            redirectAttributes.addFlashAttribute("successMessage", "Маркер успешно удалён.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Маркер с ID " + id + " не найден.");
        }
        return "redirect:/admin/organizations";
    }

    @GetMapping("/markers/api/{id}")
    @ResponseBody
    public Marker getMarkerApi(@PathVariable Long id) {
        return markerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Неверный ID маркера: " + id));
    }

    @PostMapping("/profile/update-field")
    @ResponseBody
    public ResponseEntity<?> updateProfileField(@RequestBody Map<String, String> body, Authentication authentication) {
        String field = body.get("field");
        String value = body.get("value");

        if (value == null || value.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Поле не может быть пустым"));
        }

        Login admin = loginRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Администратор не найден"));

        String trimmedValue = value.trim();

        if ("name".equals(field)) {
            admin.setName(trimmedValue);
            loginRepository.save(admin);
            return ResponseEntity.ok(Map.of("value", admin.getName()));
        }

        if ("email".equals(field)) {
            String newEmail = trimmedValue.toLowerCase();
            if (!newEmail.contains("@")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Введите корректный email"));
            }
            if (!newEmail.equalsIgnoreCase(admin.getEmail()) && loginRepository.existsByEmail(newEmail)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Этот email уже используется"));
            }
            admin.setEmail(newEmail);
            loginRepository.save(admin);

            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                    admin.getEmail(), authentication.getCredentials(), authentication.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            return ResponseEntity.ok(Map.of("value", admin.getEmail()));
        }

        return ResponseEntity.badRequest().body(Map.of("message", "Неизвестное поле"));
    }

    @PostMapping("/profile/change-password")
    @ResponseBody
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, Authentication authentication) {
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");

        if (newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("message", "Пароль должен содержать не менее 8 символов"));
        }
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Пароли не совпадают"));
        }

        Login admin = loginRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Администратор не найден"));

        admin.setPassword(passwordEncoder.encode(newPassword));
        loginRepository.save(admin);

        return ResponseEntity.ok(Map.of("message", "Пароль успешно изменён"));
    }

    @GetMapping("/chats")
    public String showChatList(Model model, Authentication authentication) {
        model.addAttribute("messages", messageRepository.findAll());

        loginRepository.findByEmail(authentication.getName())
                .ifPresent(admin -> model.addAttribute("isSuperAdmin", admin.isSuperAdmin()));

        return "chat-list";
    }

    @GetMapping("/api/chat/admin/messages")
    @ResponseBody
    public List<Message> getAdminMessages() {
        return messageRepository.findAll();
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
}