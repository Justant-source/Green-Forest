package com.vgc.entity.event.photoexhibition;

import jakarta.persistence.*;

@Entity
@Table(name = "photo_exhibition_images", uniqueConstraints = @UniqueConstraint(name = "uk_exhibition_image_order", columnNames = {"submission_id", "sort_order"}))
public class PhotoExhibitionImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "submission_id", nullable = false) private PhotoExhibitionSubmission submission;
    @Column(name = "image_url", nullable = false, length = 500) private String imageUrl;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    public PhotoExhibitionImage() { }
    public PhotoExhibitionImage(PhotoExhibitionSubmission submission, String imageUrl, int sortOrder) { this.submission = submission; this.imageUrl = imageUrl; this.sortOrder = sortOrder; }
    public Long getId() { return id; } public String getImageUrl() { return imageUrl; } public int getSortOrder() { return sortOrder; } public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
