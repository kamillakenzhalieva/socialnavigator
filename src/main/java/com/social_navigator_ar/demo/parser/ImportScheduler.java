package com.social_navigator_ar.demo.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ImportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ImportScheduler.class);

    private final ImportService importService;

    public ImportScheduler(ImportService importService) {
        this.importService = importService;
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Europe/Astrakhan")
    public void nightlyRun() {
        boolean started = importService.startAsync("SCHEDULED");
        log.info(started ? "Ночной запуск парсера начат" : "Ночной запуск пропущен: парсер уже работает");
    }
}
