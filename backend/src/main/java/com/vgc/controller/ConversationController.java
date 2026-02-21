package com.vgc.controller;

import com.vgc.dto.ConversationResponse;
import com.vgc.dto.MessageRequest;
import com.vgc.dto.MessageResponse;
import com.vgc.entity.Conversation;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.service.ConversationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ConversationController(ConversationService conversationService,
                                  UserRepository userRepository,
                                  SimpMessagingTemplate messagingTemplate) {
        this.conversationService = conversationService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public Map<String, Long> startConversation(@RequestBody Map<String, String> body,
                                               Authentication authentication) {
        User user = getUser(authentication);
        String nickname = body.get("nickname");
        Conversation conv = conversationService.startConversation(user, nickname);
        return Map.of("conversationId", conv.getId());
    }

    @GetMapping
    public List<ConversationResponse> getConversations(Authentication authentication) {
        User user = getUser(authentication);
        return conversationService.getConversations(user);
    }

    @GetMapping("/{id}/messages")
    public List<MessageResponse> getMessages(@PathVariable Long id,
                                             Authentication authentication) {
        User user = getUser(authentication);
        return conversationService.getMessages(id, user);
    }

    @PostMapping("/{id}/messages")
    public MessageResponse sendMessage(@PathVariable Long id,
                                       @RequestBody MessageRequest request,
                                       Authentication authentication) {
        User user = getUser(authentication);
        MessageResponse response = conversationService.sendMessage(id, user, request.getContent());
        messagingTemplate.convertAndSend("/topic/messages/" + id, response);
        return response;
    }

    @PostMapping("/{id}/leave")
    public Map<String, String> leaveConversation(@PathVariable Long id,
                                                 Authentication authentication) {
        User user = getUser(authentication);
        conversationService.leaveConversation(id, user);
        return Map.of("status", "left");
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
