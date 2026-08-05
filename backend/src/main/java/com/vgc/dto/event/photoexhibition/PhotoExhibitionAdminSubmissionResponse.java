package com.vgc.dto.event.photoexhibition;

import com.vgc.entity.event.photoexhibition.PhotoExhibitionSubmission;

import java.util.List;

/** Admin-only projection; voter identity is intentionally not part of public responses. */
public class PhotoExhibitionAdminSubmissionResponse {
    private Long id;
    private String title;
    private String introduction;
    private String authorNickname;
    private boolean excluded;
    private String exclusionReason;
    private int voteCount;
    private String resultTier;
    private List<PhotoExhibitionSubmissionResponse.ImageResponse> images;

    public static PhotoExhibitionAdminSubmissionResponse from(PhotoExhibitionSubmission submission) {
        PhotoExhibitionAdminSubmissionResponse response = new PhotoExhibitionAdminSubmissionResponse();
        response.id = submission.getId();
        response.title = submission.getTitle();
        response.introduction = submission.getIntroduction();
        response.authorNickname = submission.getUser().getNickname();
        response.excluded = submission.isExcluded();
        response.exclusionReason = submission.getExclusionReason();
        response.voteCount = submission.getFinalVotes();
        response.resultTier = submission.getResultTier();
        response.images = submission.getImages().stream()
                .map(PhotoExhibitionSubmissionResponse.ImageResponse::from)
                .toList();
        return response;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getIntroduction() { return introduction; }
    public String getAuthorNickname() { return authorNickname; }
    public boolean isExcluded() { return excluded; }
    public String getExclusionReason() { return exclusionReason; }
    public int getVoteCount() { return voteCount; }
    public String getResultTier() { return resultTier; }
    public List<PhotoExhibitionSubmissionResponse.ImageResponse> getImages() { return images; }
}
