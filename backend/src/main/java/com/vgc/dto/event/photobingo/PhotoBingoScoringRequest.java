package com.vgc.dto.event.photobingo;

public class PhotoBingoScoringRequest {
    private String scoreStatus; // APPROVED | REJECTED
    private String comment;

    public String getScoreStatus() { return scoreStatus; }
    public void setScoreStatus(String scoreStatus) { this.scoreStatus = scoreStatus; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
