package com.social_navigator_ar.demo.controller;

import com.social_navigator_ar.demo.model.Login;
import com.social_navigator_ar.demo.repository.LoginRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/admin/admins")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final LoginRepository loginRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminController(LoginRepository loginRepository, PasswordEncoder passwordEncoder) {
        this.loginRepository = loginRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String adminManagementPage(Model model, Authentication authentication) {
        String email = authentication.getName();
        Login currentAdmin = loginRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Администратор не найден: " + email));

        List<Login> admins = loginRepository.findByIsAdminTrue();
        admins.sort(Comparator.comparing(Login::getName, String.CASE_INSENSITIVE_ORDER));

        model.addAttribute("adminUser", currentAdmin);
        model.addAttribute("isSuperAdmin", true);
        model.addAttribute("admins", admins);
        model.addAttribute("currentAdminId", currentAdmin.getId());

        return "admin-management";
    }

    @PostMapping("/add")
    public String addAdmin(@RequestParam String name,
                            @RequestParam String email,
                            @RequestParam String password,
                            @RequestParam String confirmPassword,
                            RedirectAttributes redirectAttributes) {

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("adminFormError", "Имя администратора не может быть пустым.");
            return "redirect:/admin/admins";
        }

        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            redirectAttributes.addFlashAttribute("adminFormError", "Введите корректный email администратора.");
            return "redirect:/admin/admins";
        }

        if (password == null || password.length() < 8) {
            redirectAttributes.addFlashAttribute("adminFormError", "Пароль должен содержать не менее 8 символов.");
            return "redirect:/admin/admins";
        }

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("adminFormError", "Пароли не совпадают.");
            return "redirect:/admin/admins";
        }

        if (loginRepository.existsByEmail(email.trim().toLowerCase())) {
            redirectAttributes.addFlashAttribute("adminFormError", "Пользователь с таким email уже существует.");
            return "redirect:/admin/admins";
        }

        Login newAdmin = new Login();
        newAdmin.setName(name.trim());
        newAdmin.setEmail(email.trim().toLowerCase());
        newAdmin.setPassword(passwordEncoder.encode(password));
        newAdmin.setAdmin(true);

        try {
            loginRepository.save(newAdmin);
            redirectAttributes.addFlashAttribute("adminFormSuccess", "Новый администратор «" + newAdmin.getName() + "» успешно добавлен.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("adminFormError", "Ошибка при добавлении администратора: " + e.getMessage());
        }

        return "redirect:/admin/admins";
    }

    @PostMapping("/revoke")
    public String revokeAdmin(@RequestParam Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        Login current = loginRepository.findByEmail(authentication.getName()).orElse(null);

        if (current != null && current.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("adminFormError", "Нельзя отозвать права у самого себя.");
            return "redirect:/admin/admins";
        }

        Login target = loginRepository.findById(id).orElse(null);
        if (target == null || !target.isAdmin()) {
            redirectAttributes.addFlashAttribute("adminFormError", "Администратор не найден.");
            return "redirect:/admin/admins";
        }

        if (target.isSuperAdmin() && loginRepository.countBySuperAdminTrue() <= 1) {
            redirectAttributes.addFlashAttribute("adminFormError", "Нельзя отозвать права у единственного супер-администратора.");
            return "redirect:/admin/admins";
        }

        target.setAdmin(false);
        target.setSuperAdmin(false);
        loginRepository.save(target);

        redirectAttributes.addFlashAttribute("adminFormSuccess", "Права администратора у «" + target.getName() + "» отозваны.");
        return "redirect:/admin/admins";
    }

    @PostMapping("/promote")
    public String promoteToSuperAdmin(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        Login target = loginRepository.findById(id).orElse(null);
        if (target == null || !target.isAdmin()) {
            redirectAttributes.addFlashAttribute("adminFormError", "Администратор не найден.");
            return "redirect:/admin/admins";
        }

        target.setSuperAdmin(true);
        loginRepository.save(target);

        redirectAttributes.addFlashAttribute("adminFormSuccess", "«" + target.getName() + "» назначен супер-администратором.");
        return "redirect:/admin/admins";
    }

    @PostMapping("/demote")
    public String demoteFromSuperAdmin(@RequestParam Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        Login current = loginRepository.findByEmail(authentication.getName()).orElse(null);

        if (current != null && current.getId().equals(id) && loginRepository.countBySuperAdminTrue() <= 1) {
            redirectAttributes.addFlashAttribute("adminFormError", "Нельзя понизить единственного супер-администратора (в том числе себя).");
            return "redirect:/admin/admins";
        }

        Login target = loginRepository.findById(id).orElse(null);
        if (target == null || !target.isSuperAdmin()) {
            redirectAttributes.addFlashAttribute("adminFormError", "Супер-администратор не найден.");
            return "redirect:/admin/admins";
        }

        if (loginRepository.countBySuperAdminTrue() <= 1) {
            redirectAttributes.addFlashAttribute("adminFormError", "Нельзя понизить единственного супер-администратора.");
            return "redirect:/admin/admins";
        }

        target.setSuperAdmin(false);
        loginRepository.save(target);

        redirectAttributes.addFlashAttribute("adminFormSuccess", "«" + target.getName() + "» понижен до обычного администратора.");
        return "redirect:/admin/admins";
    }
}
