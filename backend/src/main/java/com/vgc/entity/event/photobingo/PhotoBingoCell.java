package com.vgc.entity.event.photobingo;

import com.vgc.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "photo_bingo_cells",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pbc_submission_cell", columnNames = {"submission_id", "cell_index"})
    },
    indexes = {
        @Index(name = "idx_pbc_score_status", columnList = "score_status")
    })
public class PhotoBingoCell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private PhotoBingoSubmission submission;

    @Column(name = "cell_index", nullable = false)
    private int cellIndex;

    @Column(nullable = false, length = 100)
    private String theme;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_status", nullable = false, length = 50)
    private CellScoreStatus scoreStatus = CellScoreStatus.PENDING;

    @Column(name = "score_comment", length = 200)
    private String scoreComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scored_by")
    private User scoredBy;

    @Column(name = "scored_at")
    private LocalDateTime scoredAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PhotoBingoSubmission getSubmission() { return submission; }
    public void setSubmission(PhotoBingoSubmission submission) { this.submission = submission; }
    public int getCellIndex() { return cellIndex; }
    public void setCellIndex(int cellIndex) { this.cellIndex = cellIndex; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public CellScoreStatus getScoreStatus() { return scoreStatus; }
    public void setScoreStatus(CellScoreStatus scoreStatus) { this.scoreStatus = scoreStatus; }
    public String getScoreComment() { return scoreComment; }
    public void setScoreComment(String scoreComment) { this.scoreComment = scoreComment; }
    public User getScoredBy() { return scoredBy; }
    public void setScoredBy(User scoredBy) { this.scoredBy = scoredBy; }
    public LocalDateTime getScoredAt() { return scoredAt; }
    public void setScoredAt(LocalDateTime scoredAt) { this.scoredAt = scoredAt; }
}
