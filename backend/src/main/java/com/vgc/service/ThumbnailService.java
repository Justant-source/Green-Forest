package com.vgc.service;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/**
 * 원본 이미지로부터 반응형 썸네일(sm/md)을 on-demand 로 생성한다.
 *
 * - sm: 최대 400px 장축, JPEG 0.72 품질 (모바일 광장 썸네일)
 * - md: 최대 900px 장축, JPEG 0.82 품질 (PC 광장 썸네일 / 상세)
 *
 * 파일명 규약: 원본 `/uploads/<id>.jpg` → 썸네일 `/uploads/.thumbs/<id>__sm.jpg`
 * 충돌 방지를 위해 별도 서브 디렉토리 `.thumbs` 아래 보관한다.
 */
@Service
public class ThumbnailService {

    private static final Logger log = LoggerFactory.getLogger(ThumbnailService.class);
    private static final String THUMB_DIR = ".thumbs";

    @Value("${file.upload-dir:/app/uploads}")
    private String uploadDir;

    public enum Size {
        SM(400, 0.72f),
        MD(900, 0.82f);

        final int maxDim;
        final float quality;
        Size(int maxDim, float quality) {
            this.maxDim = maxDim;
            this.quality = quality;
        }
    }

    public static Size parseSize(String raw) {
        if (raw == null) return null;
        return switch (raw.toLowerCase()) {
            case "sm" -> Size.SM;
            case "md" -> Size.MD;
            default -> null;
        };
    }

    public File thumbFile(String originalFilename, Size size) {
        String base = stripExt(originalFilename);
        File dir = new File(uploadDir, THUMB_DIR);
        return new File(dir, base + "__" + size.name().toLowerCase() + ".jpg");
    }

    /**
     * 원본으로부터 썸네일을 생성한다. 이미 있으면 그대로 반환.
     * 이미지가 아니면 null 반환 (호출측이 원본 그대로 서빙).
     */
    public File getOrCreate(File original, Size size) {
        if (!isLikelyImage(original.getName())) return null;

        File thumb = thumbFile(original.getName(), size);
        if (thumb.exists() && thumb.isFile() && thumb.length() > 0) return thumb;

        File parent = thumb.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            log.warn("썸네일 디렉토리 생성 실패: {}", parent.getAbsolutePath());
            return null;
        }

        try {
            Thumbnails.of(original)
                    .size(size.maxDim, size.maxDim)
                    .outputFormat("jpg")
                    .outputQuality(size.quality)
                    .toFile(thumb);
            return thumb;
        } catch (IOException e) {
            log.warn("썸네일 생성 실패 original={} size={}: {}", original.getName(), size, e.getMessage());
            return null;
        }
    }

    private static boolean isLikelyImage(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
            || lower.endsWith(".png") || lower.endsWith(".webp")
            || lower.endsWith(".gif");
    }

    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
