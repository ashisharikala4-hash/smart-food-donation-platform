package com.donation.controller;

import com.donation.dto.request.MessageRequest;
import com.donation.dto.response.MessageResponse;
import com.donation.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/send/{foodId}")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long foodId,
            @Valid @RequestBody MessageRequest request,
            Principal principal) {
        return ResponseEntity.ok(chatService.sendMessage(foodId, request, principal.getName()));
    }

    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody MessageRequest request,
            Principal principal) {
        if (request.getFoodId() == null) {
            throw new IllegalArgumentException("Food ID is required");
        }
        return ResponseEntity.ok(chatService.sendMessage(request.getFoodId(), request, principal.getName()));
    }

    @GetMapping("/{foodId}")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long foodId,
            Principal principal) {
        return ResponseEntity.ok(chatService.getMessages(foodId, principal.getName()));
    }
}
