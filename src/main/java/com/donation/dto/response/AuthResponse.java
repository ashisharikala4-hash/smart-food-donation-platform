package com.donation.dto.response;

import com.donation.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String name;
    private String email;
    private String mobileNumber;
    private String state;
    private String district;
    private String fullAddress;
    private String pinCode;
    private String location;
    private String donationType;
    private String availability;
    private String preferredCategories;
    private String orgName;
    private String contactPersonName;
    private String orgType;
    private String registrationNumber;
    private Integer capacity;
    private Integer foodRequirement;
    private String acceptedCategories;
    private String storageCapacity;
    private String verificationStatus;
    private String documentUrl;
    private Boolean pickupAvailable;
    private Boolean deliveryRequired;
    private Role role;
    private boolean verified;
}
