package com.social_navigator_ar.demo.controller;

import com.social_navigator_ar.demo.model.Login;
import com.social_navigator_ar.demo.repository.LoginRepository;
// import org.springframework.beans.factory.annotation.Autowired; // Не нужна, если конструктор один
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final LoginRepository loginRepository;
    private final PasswordEncoder passwordEncoder;

    // @Autowired // Необязательна, если это единственный конструктор
    public AuthController(LoginRepository loginRepository, PasswordEncoder passwordEncoder) {
        this.loginRepository = loginRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registered", required = false) String registered,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "Неверный email или пароль.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Вы успешно вышли из системы.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Регистрация успешна! Теперь вы можете войти.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        // Передаем пустой объект, если форма привязана к th:object (хотя в вашем HTML этого нет)
        // model.addAttribute("userForm", new Login());
        // Если форма не привязана, этот вызов не обязателен
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam("confirm-password") String confirmPassword,
            Model model, // Используем Model для передачи ошибок обратно на ту же страницу
            RedirectAttributes redirectAttributes) { // Используем RedirectAttributes для сообщений после успешного редиректа

        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Имя не может быть пустым.");
            // Сохраняем введенные значения, чтобы они остались в форме
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }
        if (email == null || email.trim().isEmpty() || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            model.addAttribute("errorMessage", "Введите корректный email.");
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }
        if (password == null || password.length() < 8) {
            model.addAttribute("errorMessage", "Пароль должен содержать не менее 8 символов.");
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Пароли не совпадают.");
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }

        String trimmedEmail = email.trim().toLowerCase(); // Приводим email к нижнему регистру
        if (loginRepository.existsByEmail(trimmedEmail)) {
            model.addAttribute("errorMessage", "Пользователь с таким email уже существует.");
            model.addAttribute("name", name.trim());
            model.addAttribute("email", trimmedEmail);
            return "register";
        }

        Login newUser = new Login();
        newUser.setName(name.trim());
        newUser.setEmail(trimmedEmail);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setAdmin(false); // По умолчанию все новые пользователи не админы

        try {
            loginRepository.save(newUser);
            redirectAttributes.addAttribute("registered", "true"); // Для отображения сообщения на странице логина
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Ошибка при регистрации: " + e.getMessage());
            model.addAttribute("name", name.trim());
            model.addAttribute("email", trimmedEmail);
            return "register";
        }
    }
}