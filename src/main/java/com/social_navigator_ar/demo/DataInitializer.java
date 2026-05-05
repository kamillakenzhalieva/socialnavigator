package com.social_navigator_ar.demo;

import com.social_navigator_ar.demo.repository.MessageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final MessageRepository messageRepository;

    public DataInitializer(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Вызов метода findAll() для активации создания таблицы message
        messageRepository.findAll();
        // Можно добавить логирование для подтверждения
        System.out.println("Таблица message инициализирована.");
    }
}