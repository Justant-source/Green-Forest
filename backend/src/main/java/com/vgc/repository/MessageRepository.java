package com.vgc.repository;

import com.vgc.entity.Conversation;
import com.vgc.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    void deleteByConversation(Conversation conversation);
}
