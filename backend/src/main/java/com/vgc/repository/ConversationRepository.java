package com.vgc.repository;

import com.vgc.entity.Conversation;
import com.vgc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUser1AndUser2(User user1, User user2);

    @Query("SELECT c FROM Conversation c WHERE (c.user1 = :user AND c.user1Left = false) OR (c.user2 = :user AND c.user2Left = false) ORDER BY c.updatedAt DESC")
    List<Conversation> findActiveByUser(@Param("user") User user);
}
