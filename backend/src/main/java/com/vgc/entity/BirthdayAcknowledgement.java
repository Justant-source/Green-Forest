package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "birthday_acknowledgement",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_birthday_ack",
        columnNames = {"admin_user_id", "target_user_id", "birth_year"}
    ))
public class BirthdayAcknowledgement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private User adminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @Column(name = "birth_year", nullable = false)
    private int birthYear;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    @PrePersist
    protected void onCreate() { this.confirmedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public User getAdminUser() { return adminUser; }
    public void setAdminUser(User adminUser) { this.adminUser = adminUser; }
    public User getTargetUser() { return targetUser; }
    public void setTargetUser(User targetUser) { this.targetUser = targetUser; }
    public int getBirthYear() { return birthYear; }
    public void setBirthYear(int birthYear) { this.birthYear = birthYear; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
}
