package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_sync_runs")
public class CatalogSyncRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CatalogSyncStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "item_count")
    private Integer itemCount;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by", nullable = false)
    private User startedBy;

    @PrePersist
    protected void onCreate() {
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CatalogSyncStatus getStatus() { return status; }
    public void setStatus(CatalogSyncStatus status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public User getStartedBy() { return startedBy; }
    public void setStartedBy(User startedBy) { this.startedBy = startedBy; }
}
