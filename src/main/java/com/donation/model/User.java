package com.donation.model;

import com.donation.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String mobileNumber;
    private String state;
    private String district;
    private String fullAddress;
    private String pinCode;
    private String location;
    private String donationType;
    private String availability;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String preferredCategories;
    private String orgName;
    private String contactPersonName;
    private String orgType;
    private String registrationNumber;
    private Integer capacity;
    private Integer foodRequirement;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String acceptedCategories;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String storageCapacity;
    private String verificationStatus;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String documentUrl;
    private Boolean pickupAvailable;
    private Boolean deliveryRequired;

    @Column(nullable = false)
    private boolean verified;
}
