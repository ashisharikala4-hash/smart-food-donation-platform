package com.donation.dto.response;

import com.donation.enums.DeliveryMode;
import com.donation.enums.FoodStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeliveryResponse {
    private Long id;
    private Long foodId;
    private DeliveryMode deliveryType;
    private FoodStatus status;
    private double estimatedCost;
    private Long ngoId;
    private String ngoName;
    private String ngoContact;
    private String ngoLocation;
}
