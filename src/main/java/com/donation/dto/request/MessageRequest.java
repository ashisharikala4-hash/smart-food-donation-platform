package com.donation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageRequest {
    private Long foodId;

    @NotBlank(message = "Message content is required")
    private String content;
}
