package com.vgc.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageStorageService {
    String upload(MultipartFile file) throws IOException;
    String uploadBytes(byte[] bytes, String originalFilename) throws IOException;
    void delete(String key);
}
