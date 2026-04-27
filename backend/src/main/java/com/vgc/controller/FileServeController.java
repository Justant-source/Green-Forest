package com.vgc.controller;

import com.vgc.service.ThumbnailService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URLConnection;

@RestController
@RequestMapping("/api/media")
public class FileServeController {

    @Value("${file.upload-dir:/app/uploads}")
    private String uploadDir;

    private final ThumbnailService thumbnailService;

    public FileServeController(ThumbnailService thumbnailService) {
        this.thumbnailService = thumbnailService;
    }

    /**
     * 대소문자 구분 없이 파일을 찾아 반환한다.
     * size=sm|md 쿼리가 있으면 썸네일 버전을 on-demand 생성/서빙한다.
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename,
                                              @RequestParam(required = false) String size) {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            return noCache404();
        }

        File original = resolveFile(dir, filename);
        if (original == null) return noCache404();

        ThumbnailService.Size s = ThumbnailService.parseSize(size);
        if (s != null) {
            File thumb = thumbnailService.getOrCreate(original, s);
            if (thumb != null) return buildResponse(thumb);
            // 썸네일 생성 실패 시 원본 서빙으로 폴백
        }
        return buildResponse(original);
    }

    private File resolveFile(File dir, String filename) {
        File exact = new File(dir, filename);
        if (exact.exists() && exact.isFile()) return exact;
        File[] matches = dir.listFiles(f -> f.isFile() && f.getName().equalsIgnoreCase(filename));
        if (matches == null || matches.length == 0) return null;
        return matches[0];
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
