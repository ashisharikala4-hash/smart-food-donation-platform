package com.donation.dto.response;

import com.donation.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String mobileNumber;
    private Role role;
    private String orgName;
    private String contactPersonName;
    private String orgType;
    private String registrationNumber;
    private String location;
    private String fullAddress;
    private Boolean pickupAvailable;
    private Boolean deliveryRequired;
    private String verificationStatus;
    private boolean verified;
}
