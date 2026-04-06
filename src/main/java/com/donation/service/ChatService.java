package com.donation.service;

import com.donation.dto.request.MessageRequest;
import com.donation.dto.response.MessageResponse;

import java.util.List;

public interface ChatService {
    MessageResponse sendMessage(Long foodId, MessageRequest request, String senderEmail);
    void sendSystemMessage(Long foodId, String content);
    List<MessageResponse> getMessages(Long foodId, String userEmail);
}
