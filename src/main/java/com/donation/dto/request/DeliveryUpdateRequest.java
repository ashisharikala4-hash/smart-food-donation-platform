package com.donation.dto.request;

import com.donation.enums.FoodStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeliveryUpdateRequest {

    @NotNull(message = "Status is required")
    private FoodStatus status;
}
