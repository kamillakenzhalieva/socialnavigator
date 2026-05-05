package com.social_navigator_ar.demo.controller;

import com.social_navigator_ar.demo.model.Login;
import com.social_navigator_ar.demo.model.Marker;
import com.social_navigator_ar.demo.model.Message;
import com.social_navigator_ar.demo.repository.LoginRepository;
import com.social_navigator_ar.demo.repository.MarkerRepository;
import com.social_navigator_ar.demo.repository.MessageRepository;
import com.social_navigator_ar.demo.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    public AdminController(LoginRepository loginRepository,
                           MarkerRepository markerRepository,
                           MessageRepository messageRepository,
                           FileStorageService fileStorageService,
                           PasswordEncoder passwordEncoder) {
        this.loginRepository = loginRepository;
        this.markerRepository = markerRepository;
        this.messageRepository = messageRepository;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    // === Главная панель администратора ===
    @GetMapping("/panel")
    public String adminPanel(Model model, Authentication authentication) {
        String email = authentication.getName();
        Login adminUser = loginRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Администратор не найден: " + email));

        List<Marker> markers = markerRepository.findAll();

        model.addAttribute("adminUser", adminUser);
        model.addAttribute("markers", markers);

        if (!model.containsAttribute("newMarker")) {
            model.addAttribute("newMarker", new Marker());
        }
        if (!model.containsAttribute("editMarker")) {
            model.addAttribute("editMarker", new Marker());
        }

        return "admin-panel";
    }

    // === Добавление маркера с фото ===
    @PostMapping("/markers/add")
    public String addMarker(@Valid @ModelAttribute("newMarker") Marker marker,
                            BindingResult result,
                            @RequestParam("photo") MultipartFile photo,
                            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.newMarker", result);
            redirectAttributes.addFlashAttribute("newMarker", marker);
            return "redirect:/admin/panel#add-marker-form-section";
        }

        try {
            if (!photo.isEmpty()) {
                String imageUrl = fileStorageService.storeFile(photo);
                marker.setImageUrl(imageUrl);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при загрузке фото");
            return "redirect:/admin/panel#add-marker-form-section";
        }

        markerRepository.save(marker);
        redirectAttributes.addFlashAttribute("successMessage", "Маркер успешно добавлен");
        return "redirect:/admin/panel";
    }

    // === Редактирование маркера с фото ===
    @PostMapping("/markers/update")
    public String updateMarker(@Valid @ModelAttribute("editMarker") Marker marker,
                               BindingResult result,
                               @RequestParam(name = "photo", required = false) MultipartFile photo,
                               RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.editMarker", result);
            redirectAttributes.addFlashAttribute("editMarker", marker);
            redirectAttributes.addFlashAttribute("openEditModalForId", marker.getId());
            return "redirect:/admin/panel";
        }

        Marker existingMarker = markerRepository.findById(marker.getId())
                .orElseThrow(() -> new IllegalArgumentException("Неверный ID маркера"));

        try {
            if (photo != null && !photo.isEmpty()) {
                String imageUrl = fileStorageService.storeFile(photo);
                marker.setImageUrl(imageUrl);
            } else {
                marker.setImageUrl(existingMarker.getImageUrl());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при загрузке фото");
            return "redirect:/admin/panel";
        }

        markerRepository.save(marker);
        redirectAttributes.addFlashAttribute("successMessage", "Маркер успешно обновлён");
        return "redirect:/admin/panel";
    }

    // === Удаление маркера ===
    @PostMapping("/markers/delete")
    public String deleteMarker(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        if (markerRepository.existsById(id)) {
            markerRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Маркер успешно удалён.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Маркер с ID " + id + " не найден.");
        }
        return "redirect:/admin/panel";
    }

    // === Получить маркер как JSON ===
    @GetMapping("/markers/api/{id}")
    @ResponseBody
    public Marker getMarkerApi(@PathVariable Long id) {
        return markerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Неверный ID маркера: " + id));
    }

    // === Добавление администратора ===
    @PostMapping("/add-admin")
    public String addAdmin(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           RedirectAttributes redirectAttributes) {

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("adminFormError", "Имя администратора не может быть пустым.");
            return "redirect:/admin/panel#add-admin-form-section";
        }

        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            redirectAttributes.addFlashAttribute("adminFormError", "Введите корректный email администратора.");
            return "redirect:/admin/panel#add-admin-form-section";
        }

        if (password == null || password.length() < 8) {
            redirectAttributes.addFlashAttribute("adminFormError", "Пароль должен содержать не менее 8 символов.");
            return "redirect:/admin/panel#add-admin-form-section";
        }

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("adminFormError", "Пароли не совпадают.");
            return "redirect:/admin/panel#add-admin-form-section";
        }

        if (loginRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("adminFormError", "Пользователь с таким email уже существует.");
            return "redirect:/admin/panel#add-admin-form-section";
        }

        Login newAdmin = new Login();
        newAdmin.setName(name.trim());
        newAdmin.setEmail(email.trim().toLowerCase());
        newAdmin.setPassword(passwordEncoder.encode(password)); // Предполагается, что пароль уже захэширован на уровне сервиса
        newAdmin.setAdmin(true);

        try {
            loginRepository.save(newAdmin);
            redirectAttributes.addFlashAttribute("adminFormSuccess", "Новый администратор '" + newAdmin.getName() + "' успешно добавлен.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("adminFormError", "Ошибка при добавлении администратора: " + e.getMessage());
        }

        return "redirect:/admin/panel#add-admin-form-section";
    }

    // === Список чатов ===
    @GetMapping("/chats")
    public String showChatList(Model model) {
        model.addAttribute("messages", messageRepository.findAll());
        return "chat-list";
    }

    // === API для чатов ===
    @GetMapping("/api/chat/admin/messages")
    @ResponseBody
    public List<Message> getAdminMessages() {
        return messageRepository.findAll();
    }

    // === Обработка ошибок валидации ===
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