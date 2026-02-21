package com.vgc.service;

import com.vgc.dto.ConversationResponse;
import com.vgc.dto.MessageResponse;
import com.vgc.entity.Conversation;
import com.vgc.entity.Message;
import com.vgc.entity.User;
import com.vgc.repository.ConversationRepository;
import com.vgc.repository.MessageRepository;
import com.vgc.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Conversation startConversation(User currentUser, String otherNickname) {
        User otherUser = userRepository.findByNickname(otherNickname)
                .orElseThrow(() -> new RuntimeException("User not found: " + otherNickname));

        if (currentUser.getId().equals(otherUser.getId())) {
            throw new RuntimeException("Cannot start conversation with yourself");
        }

        // 정규화: user1.id < user2.id
        User user1 = currentUser.getId() < otherUser.getId() ? currentUser : otherUser;
        User user2 = currentUser.getId() < otherUser.getId() ? otherUser : currentUser;

        Optional<Conversation> existing = conversationRepository.findByUser1AndUser2(user1, user2);
        if (existing.isPresent()) {
            Conversation conv = existing.get();
            // 나갔던 유저면 복귀
            if (conv.getUser1().getId().equals(currentUser.getId()) && conv.isUser1Left()) {
                conv.setUser1Left(false);
            } else if (conv.getUser2().getId().equals(currentUser.getId()) && conv.isUser2Left()) {
                conv.setUser2Left(false);
            }
            return conversationRepository.save(conv);
        }

        Conversation conv = new Conversation();
        conv.setUser1(user1);
        conv.setUser2(user2);
        return conversationRepository.save(conv);
    }

    public List<ConversationResponse> getConversations(User currentUser) {
        List<Conversation> conversations = conversationRepository.findActiveByUser(currentUser);
        return conversations.stream().map(conv -> {
            List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(conv);
            String lastMsg = messages.isEmpty() ? "" : messages.get(messages.size() - 1).getContent();
            return ConversationResponse.from(conv, currentUser, lastMsg);
        }).collect(Collectors.toList());
    }

    public List<MessageResponse> getMessages(Long conversationId, User currentUser) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        validateParticipant(conv, currentUser);

        List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(conv);
        return messages.stream().map(MessageResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse sendMessage(Long conversationId, User sender, String content) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        validateParticipant(conv, sender);

        // 상대가 나간 상태면 전송 차단
        boolean isUser1 = conv.getUser1().getId().equals(sender.getId());
        if (isUser1 && conv.isUser2Left()) {
            throw new RuntimeException("상대방이 대화를 나갔습니다.");
        }
        if (!isUser1 && conv.isUser1Left()) {
            throw new RuntimeException("상대방이 대화를 나갔습니다.");
        }

        Message message = new Message();
        message.setConversation(conv);
        message.setSender(sender);
        message.setContent(content);
        message.setSystemMessage(false);
        messageRepository.save(message);

        conv.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conv);

        return MessageResponse.from(message);
    }

    @Transactional
    public void leaveConversation(Long conversationId, User currentUser) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        validateParticipant(conv, currentUser);

        boolean isUser1 = conv.getUser1().getId().equals(currentUser.getId());

        // 시스템 메시지 추가
        Message sysMsg = new Message();
        sysMsg.setConversation(conv);
        sysMsg.setSender(null);
        sysMsg.setContent(currentUser.getNickname() + "님이 나갔습니다.");
        sysMsg.setSystemMessage(true);
        messageRepository.save(sysMsg);

        if (isUser1) {
            conv.setUser1Left(true);
        } else {
            conv.setUser2Left(true);
        }

        conv.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conv);

        // 양쪽 다 나갔으면 전체 삭제
        if (conv.isUser1Left() && conv.isUser2Left()) {
            messageRepository.deleteByConversation(conv);
            conversationRepository.delete(conv);
        }
    }

    private void validateParticipant(Conversation conv, User user) {
        if (!conv.getUser1().getId().equals(user.getId()) &&
                !conv.getUser2().getId().equals(user.getId())) {
            throw new RuntimeException("Not a participant of this conversation");
        }
    }
}
