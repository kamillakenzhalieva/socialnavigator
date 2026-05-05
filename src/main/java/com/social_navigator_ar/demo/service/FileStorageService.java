package com.social_navigator_ar.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

import javax.imageio.ImageIO;

@Service
public class FileStorageService {

    @Value("${upload.path}")
    private String uploadDir;

    // Размеры для обрезки
    private static final int TARGET_WIDTH = 800;
    private static final int TARGET_HEIGHT = 600;

    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            System.out.println("📁 Папка загрузки: " + uploadPath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать папку для загрузки", e);
        }
    }

    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Не удалось загрузить пустой файл.");
        }

        // Очистка имени файла
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Имя файла недопустимо");
        }

        // Генерация уникального имени
        String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9.\\-]", "_");
        String uniqueFilename = UUID.randomUUID() + "-" + safeFilename;

        // Обрезка изображения
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        BufferedImage resizedImage = resizeImage(originalImage, TARGET_WIDTH, TARGET_HEIGHT);

        // Сохраняем обрезанное изображение
        String fileExtension = getFileExtension(safeFilename);
        Path targetLocation = Paths.get(uploadDir, uniqueFilename);

        ImageIO.write(resizedImage, fileExtension, targetLocation.toFile());

        return "/uploads/" + uniqueFilename;
    }

    // Метод обрезки изображения
    private BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        resizedImage.getGraphics().drawImage(
                originalImage.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH),
                0,
                0,
                null
        );
        return resizedImage;
    }

    // Получаем расширение файла
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "png" : filename.substring(dotIndex + 1);
    }
}