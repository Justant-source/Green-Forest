package com.vgc.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URLConnection;
import java.util.Arrays;

@RestController
@RequestMapping("/api/media")
public class FileServeController {

    @Value("${file.upload-dir:/app/uploads}")
    private String uploadDir;

    /**
     * 대소문자 구분 없이 파일을 찾아 반환한다.
     * 기업 프록시망에서 URL이 소문자로 정규화되어도 (예: .PNG → .png) 정상 서빙.
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            return noCache404();
        }

        // 정확히 일치하는 파일 먼저 시도
        File exact = new File(dir, filename);
        if (exact.exists() && exact.isFile()) {
            return buildResponse(exact);
        }

        // 대소문자 구분 없이 검색
        File[] matches = dir.listFiles(f -> f.isFile() && f.getName().equalsIgnoreCase(filename));
        if (matches == null || matches.length == 0) {
            return noCache404();
        }

        return buildResponse(matches[0]);
    }

    private ResponseEntity<Resource> buildResponse(File file) {
        Resource resource = new FileSystemResource(file);
        String contentType = URLConnection.guessContentTypeFromName(file.getName());
        if (contentType == null) contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=604800, immutable")
                .body(resource);
    }

    private ResponseEntity<Resource> noCache404() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .header("Pragma", "no-cache")
                .build();
    }
}
