package com.vgc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_party", columnList = "party_id"),
    @Index(name = "idx_users_nickname", columnList = "nickname"),
    @Index(name = "idx_users_total_drops", columnList = "total_drops DESC")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    private String role = "USER";

    // --- 그린 포레스트 신규 필드 ---

    @Enumerated(EnumType.STRING)
    @Column(name = "plant_type")
    private PlantType plantType;

    @Column(name = "plant_name", length = 50)
    private String plantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_class")
    private JobClass jobClass;

    @Enumerated(EnumType.STRING)
    private Element element;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Column(name = "exp_multiplier", precision = 3, scale = 2)
    private BigDecimal expMultiplier = new BigDecimal("1.00");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private Party party;

    @Column(name = "total_drops", nullable = false)
    private int totalDrops = 0;

    // --- 기존 필드 ---

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public PlantType getPlantType() { return plantType; }
    public void setPlantType(PlantType plantType) { this.plantType = plantType; }
    public String getPlantName() { return plantName; }
    public void setPlantName(String plantName) { this.plantName = plantName; }
    public JobClass getJobClass() { return jobClass; }
    public void setJobClass(JobClass jobClass) { this.jobClass = jobClass; }
    public Element getElement() { return element; }
    public void setElement(Element element) { this.element = element; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public BigDecimal getExpMultiplier() { return expMultiplier; }
    public void setExpMultiplier(BigDecimal expMultiplier) { this.expMultiplier = expMultiplier; }
    public Party getParty() { return party; }
    public void setParty(Party party) { this.party = party; }
    public int getTotalDrops() { return totalDrops; }
    public void setTotalDrops(int totalDrops) { this.totalDrops = totalDrops; }
}
