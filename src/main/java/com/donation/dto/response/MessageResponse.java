package com.donation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {
    private Long id;
    private Long foodId;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private String content;
    private boolean isSystem;
    private LocalDateTime timestamp;
}
