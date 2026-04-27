package com.vgc.entity.event;

import com.vgc.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_participations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_qep_event_user", columnNames = {"event_id", "user_id"})
    })
public class EventParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "total_drops_awarded", nullable = false)
    private int totalDropsAwarded = 0;

    @Column(name = "awarded_at")
    private LocalDateTime awardedAt;

    @PrePersist
    protected void onCreate() {
        if (this.joinedAt == null) this.joinedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public int getTotalDropsAwarded() { return totalDropsAwarded; }
    public void setTotalDropsAwarded(int totalDropsAwarded) { this.totalDropsAwarded = totalDropsAwarded; }
    public LocalDateTime getAwardedAt() { return awardedAt; }
    public void setAwardedAt(LocalDateTime awardedAt) { this.awardedAt = awardedAt; }
}
