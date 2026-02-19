package com.vgc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Profile("local")
public class LocalStorageService implements ImageStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public String upload(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), uploadPath.resolve(fileName));
        return "/uploads/" + fileName;
    }

    @Override
    public void delete(String key) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(key.replaceFirst("^/uploads/", ""));
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + key, e);
        }
    }
}
