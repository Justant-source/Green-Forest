package com.vgc.dto.event.photobingo;

import java.time.LocalDateTime;

public class PhotoBingoActivityResponse {
    private Long userId;
    private String userNickname;
    private int cellIndex;
    private String theme;
    private LocalDateTime uploadedAt;
    private int uploadedCount;

    public PhotoBingoActivityResponse() {}

    public PhotoBingoActivityResponse(Long userId, String userNickname, int cellIndex,
                                      String theme, LocalDateTime uploadedAt, int uploadedCount) {
        this.userId = userId;
        this.userNickname = userNickname;
        this.cellIndex = cellIndex;
        this.theme = theme;
        this.uploadedAt = uploadedAt;
        this.uploadedCount = uploadedCount;
    }

    public Long getUserId() { return userId; }
    public String getUserNickname() { return userNickname; }
    public int getCellIndex() { return cellIndex; }
    public String getTheme() { return theme; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public int getUploadedCount() { return uploadedCount; }
}
