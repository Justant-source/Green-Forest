package com.vgc.entity.event.photoexhibition;

import com.vgc.entity.event.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Separate table config leaves the legacy PhotoBingo JSON converter untouched. */
@Entity
@Table(name = "photo_exhibition_configs")
public class PhotoExhibitionConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(name = "submission_start", nullable = false)
    private LocalDateTime submissionStart;

    @Column(name = "submission_end", nullable = false)
    private LocalDateTime submissionEnd;

    @Column(name = "review_end", nullable = false)
    private LocalDateTime reviewEnd;

    @Column(name = "voting_end", nullable = false)
    private LocalDateTime votingEnd;

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public LocalDateTime getSubmissionStart() { return submissionStart; }
    public void setSubmissionStart(LocalDateTime submissionStart) { this.submissionStart = submissionStart; }
    public LocalDateTime getSubmissionEnd() { return submissionEnd; }
    public void setSubmissionEnd(LocalDateTime submissionEnd) { this.submissionEnd = submissionEnd; }
    public LocalDateTime getReviewEnd() { return reviewEnd; }
    public void setReviewEnd(LocalDateTime reviewEnd) { this.reviewEnd = reviewEnd; }
    public LocalDateTime getVotingEnd() { return votingEnd; }
    public void setVotingEnd(LocalDateTime votingEnd) { this.votingEnd = votingEnd; }
}
