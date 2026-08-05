package com.vgc.entity.event.photoexhibition;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "photo_exhibition_reward_grants", uniqueConstraints = @UniqueConstraint(name = "uk_exhibition_reward_once", columnNames = {"event_id", "user_id", "grant_kind"}))
public class PhotoExhibitionRewardGrant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "event_id", nullable = false) private com.vgc.entity.event.Event event;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private com.vgc.entity.User user;
    @Column(name = "grant_kind", nullable = false, length = 30) private String grantKind;
    @Column(nullable = false) private int amount;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate(){createdAt=LocalDateTime.now();}
    public PhotoExhibitionRewardGrant(){} public PhotoExhibitionRewardGrant(com.vgc.entity.event.Event e,com.vgc.entity.User u,String k,int a){event=e;user=u;grantKind=k;amount=a;}
}
