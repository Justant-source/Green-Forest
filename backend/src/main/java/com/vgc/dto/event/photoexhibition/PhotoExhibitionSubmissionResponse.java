package com.vgc.dto.event.photoexhibition;

import com.vgc.entity.event.photoexhibition.PhotoExhibitionImage;
import com.vgc.entity.event.photoexhibition.PhotoExhibitionSubmission;

import java.util.List;

/** Public projection. It intentionally never exposes JPA relationships or voter identities. */
public class PhotoExhibitionSubmissionResponse {
    private Long id;
    private String title;
    private String introduction;
    private List<ImageResponse> images;
    private boolean mine;
    private String authorNickname;
    private Integer finalVotes;
    private String resultTier;

    public static PhotoExhibitionSubmissionResponse from(PhotoExhibitionSubmission submission,
                                                          boolean mine, boolean resultPublished) {
        PhotoExhibitionSubmissionResponse response = new PhotoExhibitionSubmissionResponse();
        response.id = submission.getId();
        response.title = submission.getTitle();
        response.introduction = submission.getIntroduction();
        response.images = submission.getImages().stream().map(ImageResponse::from).toList();
        response.mine = mine;
        if (resultPublished) {
            response.authorNickname = submission.getUser().getNickname();
            response.finalVotes = submission.getFinalVotes();
            response.resultTier = submission.getResultTier();
        }
        return response;
    }
    public Long getId() { return id; } public String getTitle() { return title; }
    public String getIntroduction() { return introduction; } public List<ImageResponse> getImages() { return images; }
    public boolean isMine() { return mine; } public String getAuthorNickname() { return authorNickname; }
    public Integer getFinalVotes() { return finalVotes; } public String getResultTier() { return resultTier; }

    public static class ImageResponse {
        private Long id; private String imageUrl; private int sortOrder; private boolean representative;
        public static ImageResponse from(PhotoExhibitionImage image) { ImageResponse r = new ImageResponse(); r.id=image.getId(); r.imageUrl=image.getImageUrl(); r.sortOrder=image.getSortOrder(); r.representative=image.getSortOrder()==0; return r; }
        public Long getId(){return id;} public String getImageUrl(){return imageUrl;} public int getSortOrder(){return sortOrder;} public boolean isRepresentative(){return representative;}
    }
}
