package com.vgc.dto.event.photoexhibition;

import com.vgc.entity.event.photoexhibition.PhotoExhibitionConfig;

import java.time.LocalDateTime;

public class PhotoExhibitionConfigResponse {
    private LocalDateTime submissionStart;
    private LocalDateTime submissionEnd;
    private LocalDateTime reviewEnd;
    private LocalDateTime votingEnd;

    public static PhotoExhibitionConfigResponse from(PhotoExhibitionConfig config) {
        PhotoExhibitionConfigResponse response = new PhotoExhibitionConfigResponse();
        response.submissionStart = config.getSubmissionStart();
        response.submissionEnd = config.getSubmissionEnd();
        response.reviewEnd = config.getReviewEnd();
        response.votingEnd = config.getVotingEnd();
        return response;
    }
    public LocalDateTime getSubmissionStart() { return submissionStart; }
    public LocalDateTime getSubmissionEnd() { return submissionEnd; }
    public LocalDateTime getReviewEnd() { return reviewEnd; }
    public LocalDateTime getVotingEnd() { return votingEnd; }
}
