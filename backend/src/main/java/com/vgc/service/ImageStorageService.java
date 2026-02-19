package com.vgc.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageStorageService {
    String upload(MultipartFile file) throws IOException;
    void delete(String key);
}
