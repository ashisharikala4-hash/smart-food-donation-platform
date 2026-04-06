package com.donation.dto.response;

import com.donation.enums.DonationCategory;
import com.donation.enums.DonationCondition;
import com.donation.enums.DeliveryMode;
import com.donation.enums.FoodStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FoodResponse {
    private Long id;
    private Long donorId;
    private String donorName;
    private String donorContact;
    private String title;
    private String description;
    private DonationCategory category;
    private DonationCategory foodType;
    private String type;
    private int quantity;
    private DonationCondition condition;
    private LocalDateTime expiryTime;
    private String images;
    private String location;
    private DeliveryMode deliveryMode;
    private DeliveryMode donorDeliveryPreference;
    private DeliveryMode organizationDeliveryPreference;
    private Boolean deliveryAgreementReached;
    private FoodStatus status;
    private Long acceptedById;
    private String acceptedByName;
    private String acceptedByContact;
    private String acceptedByLocation;
    private String acceptedByAddress;
    private Boolean acceptedByPickupAvailable;
    private String organizationName;
    private String ngoName;
    private LocalDateTime createdAt;
    private List<String> suitability;
    private Double aiScore;
    private Boolean recommended;
}
