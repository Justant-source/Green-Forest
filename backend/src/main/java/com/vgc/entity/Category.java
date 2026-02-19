package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String label;

    private String color;

    private boolean hasStatus = false;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Category() {}

    public Category(String name, String label, String color, boolean hasStatus) {
        this.name = name;
        this.label = label;
        this.color = color;
        this.hasStatus = hasStatus;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public boolean isHasStatus() { return hasStatus; }
    public void setHasStatus(boolean hasStatus) { this.hasStatus = hasStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
