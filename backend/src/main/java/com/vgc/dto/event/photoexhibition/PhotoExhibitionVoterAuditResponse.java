package com.vgc.dto.event.photoexhibition;

import com.vgc.entity.event.photoexhibition.PhotoExhibitionVote;

public class PhotoExhibitionVoterAuditResponse {
    private Long submissionId;
    private String voterNickname;
    private String workTitle;
    public static PhotoExhibitionVoterAuditResponse from(PhotoExhibitionVote vote) { PhotoExhibitionVoterAuditResponse r = new PhotoExhibitionVoterAuditResponse(); r.submissionId=vote.getSubmission().getId(); r.voterNickname=vote.getVoter().getNickname(); r.workTitle=vote.getSubmission().getTitle(); return r; }
    public Long getSubmissionId(){return submissionId;} public String getVoterNickname(){return voterNickname;} public String getWorkTitle(){return workTitle;}
}
