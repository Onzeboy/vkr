package com.nzby.homeshop.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private final Path uploadDir = Paths.get("src/main/resources/static/uploads");

    public FileStorageService() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            logger.error("Не удалось создать директорию для загрузки файлов: {}", uploadDir, e);
            throw new RuntimeException("Не удалось инициализировать директорию для загрузки файлов", e);
        }
    }

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            logger.warn("Попытка сохранить пустой файл");
            return null;
        }

        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            logger.info("Файл сохранен: {}", filePath);
            return "/uploads/" + fileName;
        } catch (IOException e) {
            logger.error("Ошибка при сохранении файла: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Не удалось сохранить файл", e);
        }
    }
}