package com.vgc.entity.event.photoexhibition;
import jakarta.persistence.*; import java.time.LocalDateTime; import java.util.*;
@Entity @Table(name="photo_exhibition_submissions", indexes=@Index(name="idx_exhibition_submission_event",columnList="event_id"), uniqueConstraints=@UniqueConstraint(name="uk_photo_exhibition_event_user",columnNames={"event_id","user_id"}))
public class PhotoExhibitionSubmission {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="event_id",nullable=false) private com.vgc.entity.event.Event event;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private com.vgc.entity.User user;
 @Column(nullable=false,length=150) private String title=""; @Column(columnDefinition="TEXT") private String introduction="";
 @Column(nullable=false) private boolean excluded=false; @Column(name="exclusion_reason",length=500) private String exclusionReason;
 @Column(name="final_votes",nullable=false) private int finalVotes; @Column(name="result_tier",length=20) private String resultTier;
 @Column(name="plaza_post_id") private Long plazaPostId; @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @OneToMany(mappedBy="submission",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("sortOrder ASC") private List<PhotoExhibitionImage> images=new ArrayList<>();
 @PrePersist void created(){createdAt=LocalDateTime.now();}
 public Long getId(){return id;} public com.vgc.entity.event.Event getEvent(){return event;} public void setEvent(com.vgc.entity.event.Event x){event=x;} public com.vgc.entity.User getUser(){return user;} public void setUser(com.vgc.entity.User x){user=x;}
 public String getTitle(){return title;} public void setTitle(String x){title=x;} public String getIntroduction(){return introduction;} public void setIntroduction(String x){introduction=x;} public boolean isExcluded(){return excluded;} public void setExcluded(boolean x){excluded=x;} public String getExclusionReason(){return exclusionReason;} public void setExclusionReason(String x){exclusionReason=x;} public int getFinalVotes(){return finalVotes;} public void setFinalVotes(int x){finalVotes=x;} public String getResultTier(){return resultTier;} public void setResultTier(String x){resultTier=x;} public Long getPlazaPostId(){return plazaPostId;} public void setPlazaPostId(Long x){plazaPostId=x;} public List<PhotoExhibitionImage> getImages(){return images;}
 public boolean isValid(){return !excluded && !images.isEmpty() && title!=null&&!title.isBlank()&&introduction!=null&&!introduction.isBlank();}
}
