package com.vgc.dto.event.photobingo;

import com.vgc.entity.event.photobingo.PhotoBingoCell;
import com.vgc.entity.event.photobingo.PhotoBingoSubmission;

import java.util.Comparator;
import java.util.List;

public class PhotoBingoSubmissionResponse {

    private Long submissionId;
    private Long userId;
    private String userNickname;
    private String caption;
    private List<PhotoBingoCellResponse> cells;
    private int achievedLines;
    private int finalRewardDrops;

    public static PhotoBingoSubmissionResponse from(PhotoBingoSubmission sub) {
        PhotoBingoSubmissionResponse r = new PhotoBingoSubmissionResponse();
        r.submissionId = sub.getId();
        r.userId = sub.getUser() != null ? sub.getUser().getId() : null;
        r.userNickname = sub.getUser() != null ? sub.getUser().getNickname() : null;
        r.caption = sub.getCaption();
        r.cells = sub.getCells().stream()
                .sorted(Comparator.comparingInt(PhotoBingoCell::getCellIndex))
                .map(PhotoBingoCellResponse::from)
                .toList();
        r.achievedLines = sub.getAchievedLines();
        r.finalRewardDrops = sub.getFinalRewardDrops();
        return r;
    }

    public Long getSubmissionId() { return submissionId; }
    public Long getUserId() { return userId; }
    public String getUserNickname() { return userNickname; }
    public String getCaption() { return caption; }
    public List<PhotoBingoCellResponse> getCells() { return cells; }
    public int getAchievedLines() { return achievedLines; }
    public int getFinalRewardDrops() { return finalRewardDrops; }
}
