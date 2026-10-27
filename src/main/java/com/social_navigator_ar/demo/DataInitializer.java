package com.social_navigator_ar.demo;

import com.social_navigator_ar.demo.model.Login;
import com.social_navigator_ar.demo.repository.LoginRepository;
import com.social_navigator_ar.demo.repository.MessageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final MessageRepository messageRepository;
    private final LoginRepository loginRepository;

    public DataInitializer(MessageRepository messageRepository, LoginRepository loginRepository) {
        this.messageRepository = messageRepository;
        this.loginRepository = loginRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        messageRepository.findAll();
        System.out.println("Таблица message инициализирована.");

        bootstrapSuperAdminIfNeeded();
    }
    private void bootstrapSuperAdminIfNeeded() {
        if (loginRepository.countBySuperAdminTrue() > 0) {
            return;
        }

        List<Login> admins = loginRepository.findByIsAdminTrue();
        admins.stream()
                .min(Comparator.comparing(Login::getId))
                .ifPresent(admin -> {
                    admin.setSuperAdmin(true);
                    loginRepository.save(admin);
                    System.out.println("Автоматически назначен супер-администратор: " + admin.getEmail());
                });
    }
}