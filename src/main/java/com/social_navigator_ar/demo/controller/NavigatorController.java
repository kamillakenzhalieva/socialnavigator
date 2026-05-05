package com.social_navigator_ar.demo.controller;

import com.social_navigator_ar.demo.model.Login;
import com.social_navigator_ar.demo.model.UserProfiles;
import com.social_navigator_ar.demo.repository.LoginRepository;
import com.social_navigator_ar.demo.repository.UserProfilesRepository;
// import org.springframework.beans.factory.annotation.Autowired; // Не нужна, если конструктор один
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
public class NavigatorController {
    private final UserProfilesRepository userProfilesRepository;
    private final LoginRepository loginRepository;

    // @Autowired // Необязательна, если это единственный конструктор
    public NavigatorController(UserProfilesRepository userProfilesRepository,
                               LoginRepository loginRepository) {
        this.userProfilesRepository = userProfilesRepository;
        this.loginRepository = loginRepository;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            String email = authentication.getName();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

            if (!isAdmin) {
                loginRepository.findByEmail(email).ifPresent(user -> {
                    Optional<UserProfiles> profileOpt = userProfilesRepository.findByUserId(user.getId());
                    boolean isProfileIncomplete;
                    if (profileOpt.isPresent()) {
                        UserProfiles profile = profileOpt.get();
                        isProfileIncomplete =
                                (profile.getPhone() == null || profile.getPhone().trim().isEmpty()) ||
                                (profile.getDiagnosis() == null || profile.getDiagnosis().trim().isEmpty());
                        // Можно добавить другие обязательные поля для проверки
                    } else {
                        isProfileIncomplete = true; // Профиля нет, значит он не заполнен
                    }

                    if (isProfileIncomplete) {
                        model.addAttribute("showFillProfileNotification", true);
                    }
                });
            }
        }
        return "index";
    }

    @GetMapping("/cabinet")
    public String cabinet(Model model, Authentication authentication,
                          @RequestParam(value = "updated", required = false) Boolean updated,
                          @RequestParam(value = "error", required = false) String error) { // Для общих ошибок, не связанных с RedirectAttributes
        // Сообщения из RedirectAttributes уже должны быть доступны в модели благодаря Spring
        // Например, successMessage или errorMessage

        String email = authentication.getName();
        Login user = loginRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + email));

        UserProfiles profile = userProfilesRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserProfiles newProfile = new UserProfiles();
                    newProfile.setUser(user);
                    // Если нужно, чтобы пустой профиль создавался при первом заходе:
                    // return userProfilesRepository.save(newProfile);
                    return newProfile;
                });

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        return "cabinet";
    }

    @PostMapping("/cabinet")
    public String updateProfile(@RequestParam Map<String, String> formData,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        String authEmail = authentication.getName();
        Login user = loginRepository.findByEmail(authEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + authEmail));

        // Обновление имени пользователя
        String newName = formData.get("name");
        if (newName != null && !newName.trim().isEmpty()) {
            user.setName(newName.trim());
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Имя не может быть пустым.");
            return "redirect:/cabinet";
        }
        loginRepository.save(user);

        // Обновление или создание профиля пользователя
        UserProfiles profile = userProfilesRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserProfiles newProfile = new UserProfiles();
                    newProfile.setUser(user);
                    return newProfile;
                });

        profile.setPhone(formData.get("phone") != null ? formData.get("phone").trim() : null);
        profile.setDiagnosis(formData.get("diagnosis") != null ? formData.get("diagnosis").trim() : null);

        String childAgeStr = formData.get("childAge");
        if (childAgeStr != null && !childAgeStr.trim().isEmpty()) {
            try {
                profile.setChildAge(Integer.valueOf(childAgeStr.trim()));
            } catch (NumberFormatException e) {
                // Можно установить null или вернуть ошибку
                profile.setChildAge(null);
                redirectAttributes.addFlashAttribute("errorMessage", "Некорректный формат возраста ребенка.");
                // return "redirect:/cabinet"; // Если это критичная ошибка
            }
        } else {
            profile.setChildAge(null); // Если поле пустое
        }

        try {
            userProfilesRepository.save(profile);
            redirectAttributes.addFlashAttribute("successMessage", "Профиль успешно обновлен.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении профиля: " + e.getMessage());
        }
        return "redirect:/cabinet";
    }
}