package com.donation.service.impl;

import com.donation.dto.request.MessageRequest;
import com.donation.dto.response.MessageResponse;
import com.donation.enums.FoodStatus;
import com.donation.enums.Role;
import com.donation.exception.ResourceNotFoundException;
import com.donation.exception.UnauthorizedException;
import com.donation.model.Food;
import com.donation.model.Message;
import com.donation.model.User;
import com.donation.repository.FoodRepository;
import com.donation.repository.MessageRepository;
import com.donation.repository.UserRepository;
import com.donation.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final MessageRepository messageRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;

    public ChatServiceImpl(MessageRepository messageRepository, FoodRepository foodRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.foodRepository = foodRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long foodId, MessageRequest request, String senderEmail) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found: " + foodId));

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + senderEmail));

        // Authorization: Only donor or accepted organization can participate in chat
        validateChatAccess(food, senderEmail);

        logger.info("Sending message from {} for food {}", senderEmail, foodId);

        Message message = Message.builder()
                .food(food)
                .sender(sender)
                .content(request.getContent())
                .timestamp(LocalDateTime.now())
                .build();

        messageRepository.save(message);
        return toResponse(message);
    }

    @Override
    @Transactional
    public void sendSystemMessage(Long foodId, String content) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found: " + foodId));

        logger.info("Sending system message for food {}: {}", foodId, content);

        Message message = Message.builder()
                .food(food)
                .sender(null) // System message has no sender
                .content(content)
                .isSystem(true)
                .timestamp(LocalDateTime.now())
                .build();

        messageRepository.save(message);
    }

    @Override
    public List<MessageResponse> getMessages(Long foodId, String userEmail) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found: " + foodId));

        validateChatAccess(food, userEmail);

        return messageRepository.findByFoodOrderByTimestampAsc(food).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateChatAccess(Food food, String userEmail) {
        if (food.getStatus() != FoodStatus.ACCEPTED
                && food.getStatus() != FoodStatus.PENDING_NGO
                && food.getStatus() != FoodStatus.PICKUP_STARTED
                && food.getStatus() != FoodStatus.DELIVERED) {
            throw new UnauthorizedException("Chat is only available while the donation is active or after delivery for final thanks");
        }

        User requestingUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        if (requestingUser.getRole() == Role.ADMIN) {
            return;
        }

        boolean isDonor = food.getDonor().getEmail().equals(userEmail);
        boolean isAcceptedOrg = food.getAcceptedBy() != null && food.getAcceptedBy().getEmail().equals(userEmail);

        if (!isDonor && !isAcceptedOrg) {
            throw new UnauthorizedException("You are not part of this donation's chat");
        }
    }

    private MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .foodId(message.getFood().getId())
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderName(message.getSender() != null ? message.getSender().getName() : "System")
                .senderRole(message.getSender() != null ? message.getSender().getRole().name() : "SYSTEM")
                .content(message.getContent())
                .isSystem(message.isSystem())
                .timestamp(message.getTimestamp())
                .build();
    }
}
