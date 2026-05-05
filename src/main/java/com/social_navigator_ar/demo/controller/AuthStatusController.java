package com.social_navigator_ar.demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthStatusController {

    @GetMapping("/check-auth")
    public Map<String, Boolean> checkAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null &&
                authentication.isAuthenticated() &&
                !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"));
        return Collections.singletonMap("authenticated", isAuthenticated);
    }

    @GetMapping("/check-admin")
    public Map<String, Object> checkAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = Collections.singletonMap("isAdmin", false);

        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            System.out.println("Роли пользователя в /check-admin: " + authentication.getAuthorities());

            response = new HashMap<>();
            response.put("isAdmin", isAdmin);
            response.put("authorities", authentication.getAuthorities());
        }

        return response;
    }
}