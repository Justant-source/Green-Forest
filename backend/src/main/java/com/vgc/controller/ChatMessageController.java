package com.vgc.controller;

import com.vgc.dto.MessageRequest;
import com.vgc.dto.MessageResponse;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.ConversationService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatMessageController {

    private final ConversationService conversationService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageController(ConversationService conversationService,
                                 UserRepository userRepository,
                                 SimpMessagingTemplate messagingTemplate) {
        this.conversationService = conversationService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/{conversationId}")
    public void handleMessage(@DestinationVariable Long conversationId,
                              @Payload MessageRequest request,
                              Principal principal) {
        User sender = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        MessageResponse response = conversationService.sendMessage(conversationId, sender, request.getContent());
        messagingTemplate.convertAndSend("/topic/messages/" + conversationId, response);
    }
}
