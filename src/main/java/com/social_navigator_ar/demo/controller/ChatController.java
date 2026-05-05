package com.social_navigator_ar.demo.controller;

import com.social_navigator_ar.demo.model.Message;
import com.social_navigator_ar.demo.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private MessageRepository messageRepository;

    @PostMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Message> sendMessage(@RequestBody Message message, Principal principal) {
        if (principal == null) {
            System.out.println("Principal is null - unauthorized access attempt");
            return ResponseEntity.status(401).build();
        }
        message.setUserId(principal.getName());
        message.setTimestamp(LocalDateTime.now());
        message.setIsAdminResponse(false);
        message.setParentMessageId(null);
        Message savedMessage = messageRepository.save(message);
        System.out.println("Sent user message: " + savedMessage);
        return ResponseEntity.ok(savedMessage);
    }

    @GetMapping("/messages/{messageId}/responses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Message>> getResponses(@PathVariable Long messageId, Principal principal) {
        Message originalMessage = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Сообщение не найдено"));

        if (!originalMessage.getUserId().equals(principal.getName())) {
            System.out.println("Access denied for user " + principal.getName() + " to message " + messageId);
            return ResponseEntity.status(403).build();
        }

        List<Message> responses = messageRepository.findByParentMessageId(messageId);
        System.out.println("Responses for message " + messageId + ": " + responses);
        return ResponseEntity.ok(responses);
    }

    @PostMapping(value = "/admin/messages/reply", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> replyToMessage(@RequestBody ReplyRequest replyRequest, Principal principal) {
        Message originalMessage = messageRepository.findById(replyRequest.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("Сообщение не найдено"));

        Message reply = new Message();
        reply.setUserId(originalMessage.getUserId()); // Устанавливаем userId оригинального сообщения
        reply.setText(replyRequest.getText());
        reply.setTimestamp(LocalDateTime.now());
        reply.setIsAdminResponse(true);
        reply.setParentMessageId(replyRequest.getMessageId());

        Message savedReply = messageRepository.save(reply);
        System.out.println("Sent admin reply: " + savedReply);
        return ResponseEntity.ok(Collections.singletonMap("success", true));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Message>> getUserChatHistory(Principal principal) {
        if (principal == null) {
            System.out.println("Principal is null - unauthorized access attempt for chat history");
            return ResponseEntity.status(401).build();
        }
        String userId = principal.getName();
        List<Message> userMessages = messageRepository.findAll().stream()
                .filter(message -> message.getUserId().equals(userId) && !message.isAdminResponse() && message.getParentMessageId() == null)
                .collect(Collectors.toList());

        List<Message> allMessages = new ArrayList<>(userMessages);
        for (Message message : userMessages) {
            List<Message> responses = messageRepository.findByParentMessageId(message.getId());
            allMessages.addAll(responses);
        }

        allMessages.sort(Comparator.comparing(Message::getTimestamp));
        System.out.println("User messages for " + userId + ": " + allMessages);
        return ResponseEntity.ok(allMessages);
    }

    @GetMapping("/admin/chats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, List<Message>>> getAdminChats() {
        List<Message> allMessages = messageRepository.findAll();
        System.out.println("All messages: " + allMessages);
        Map<String, List<Message>> chats = new LinkedHashMap<>();
        for (Message message : allMessages) {
            if (!message.isAdminResponse() && message.getParentMessageId() == null) {
                String userId = message.getUserId();
                List<Message> userMessages = chats.computeIfAbsent(userId, k -> new ArrayList<>());
                userMessages.add(message);
                List<Message> responses = messageRepository.findByParentMessageId(message.getId());
                userMessages.addAll(responses);
            }
        }

        chats.forEach((userId, messages) -> messages.sort(Comparator.comparing(Message::getTimestamp)));
        System.out.println("Chats: " + chats);
        return ResponseEntity.ok(chats);
    }
}

class ReplyRequest {
    private Long messageId;
    private String text;

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}