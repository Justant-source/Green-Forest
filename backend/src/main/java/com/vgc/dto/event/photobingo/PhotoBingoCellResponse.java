package com.vgc.dto.event.photobingo;

import com.vgc.entity.event.photobingo.PhotoBingoCell;

import java.time.LocalDateTime;

public class PhotoBingoCellResponse {

    private Long id;
    private int cellIndex;
    private String theme;
    private String imageUrl;
    private LocalDateTime uploadedAt;
    private String scoreStatus;
    private String scoreComment;

    public static PhotoBingoCellResponse from(PhotoBingoCell cell) {
        PhotoBingoCellResponse r = new PhotoBingoCellResponse();
        r.id = cell.getId();
        r.cellIndex = cell.getCellIndex();
        r.theme = cell.getTheme();
        r.imageUrl = cell.getImageUrl();
        r.uploadedAt = cell.getUploadedAt();
        r.scoreStatus = cell.getScoreStatus() != null ? cell.getScoreStatus().name() : null;
        r.scoreComment = cell.getScoreComment();
        return r;
    }

    public Long getId() { return id; }
    public int getCellIndex() { return cellIndex; }
    public String getTheme() { return theme; }
    public String getImageUrl() { return imageUrl; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public String getScoreStatus() { return scoreStatus; }
    public String getScoreComment() { return scoreComment; }
}
