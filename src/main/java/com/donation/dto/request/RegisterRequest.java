package com.donation.dto.request;


import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "A valid email address is required")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Role is required")
    private String role;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Full address is required")
    private String fullAddress;

    @NotBlank(message = "Pin code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pin code must be 6 digits")
    private String pinCode;

    private String donationType;
    private String availability;
    private String preferredCategories;
    private String orgName;
    private String contactPersonName;
    private String orgType;
    private String registrationNumber;
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;
    @Min(value = 1, message = "Food requirement must be at least 1")
    private Integer foodRequirement;
    private String acceptedCategories;
    private String storageCapacity;
    private String documentUrl;
    private Boolean pickupAvailable;
    private Boolean deliveryRequired;

    private String location;
}
