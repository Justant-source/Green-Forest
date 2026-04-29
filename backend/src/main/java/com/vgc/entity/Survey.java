package com.vgc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "surveys")
public class Survey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private Post post;

    @Column(name = "closes_at", nullable = false)
    private LocalDateTime closesAt;

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous = false;

    @Column(name = "allow_option_add_by_user", nullable = false)
    private boolean allowOptionAddByUser = false;

    @Column(name = "allow_multi_select", nullable = false)
    private boolean allowMultiSelect = false;

    @Column(name = "is_notice", nullable = false)
    private boolean notice = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, id ASC")
    private List<SurveyOption> options = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public LocalDateTime getClosesAt() { return closesAt; }
    public void setClosesAt(LocalDateTime closesAt) { this.closesAt = closesAt; }
    public boolean isAnonymous() { return anonymous; }
    public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }
    public boolean isAllowOptionAddByUser() { return allowOptionAddByUser; }
    public void setAllowOptionAddByUser(boolean allowOptionAddByUser) { this.allowOptionAddByUser = allowOptionAddByUser; }
    public boolean isAllowMultiSelect() { return allowMultiSelect; }
    public void setAllowMultiSelect(boolean allowMultiSelect) { this.allowMultiSelect = allowMultiSelect; }
    public boolean isNotice() { return notice; }
    public void setNotice(boolean notice) { this.notice = notice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<SurveyOption> getOptions() { return options; }
    public void setOptions(List<SurveyOption> options) { this.options = options; }
}
