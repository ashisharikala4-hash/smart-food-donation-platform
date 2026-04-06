package com.donation.dto.request;

import com.donation.enums.DeliveryMode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryModeRequest {
    @NotNull(message = "Food ID is required")
    private Long foodId;

    @NotNull(message = "Delivery mode is required")
    private DeliveryMode mode;
}
