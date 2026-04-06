package com.donation.service.impl;

import com.donation.dto.request.LoginRequest;
import com.donation.dto.request.RegisterRequest;
import com.donation.dto.response.AuthResponse;
import com.donation.enums.Role;
import com.donation.exception.ResourceNotFoundException;
import com.donation.exception.UserAlreadyExistsException;
import com.donation.model.User;
import com.donation.repository.UserRepository;
import com.donation.security.JwtUtil;
import com.donation.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        logger.info("Processing registration request for email: {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Registration failed: Email {} is already in use", request.getEmail());
            throw new UserAlreadyExistsException("An account with this email already exists");
        }

        Role userRole;
        try {
            userRole = Role.valueOf(request.getRole().toUpperCase());
            if (userRole == Role.ADMIN) {
                logger.warn("Security warning: Attempted registration with ADMIN role for email {}", request.getEmail());
                throw new IllegalArgumentException("Admin registration is not allowed through this endpoint");
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            logger.error("Registration failed: Invalid role '{}' for email {}", request.getRole(), request.getEmail());
            throw new IllegalArgumentException("Invalid role provided. Must be DONOR or ORGANIZATION.");
        }

        // Organizations await admin approval; all other roles are auto-verified
        boolean autoVerified = userRole != Role.ORGANIZATION;
        validateRoleSpecificFields(request, userRole);

        User user = User.builder()
                .name(userRole == Role.ORGANIZATION ? request.getOrgName() : request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .mobileNumber(request.getMobileNumber())
                .state(request.getState())
                .district(request.getDistrict())
                .fullAddress(request.getFullAddress())
                .pinCode(request.getPinCode())
                .location(request.getLocation())
                .donationType(request.getDonationType())
                .availability(request.getAvailability())
                .preferredCategories(request.getPreferredCategories())
                .orgName(request.getOrgName())
                .contactPersonName(request.getContactPersonName())
                .orgType(request.getOrgType())
                .registrationNumber(request.getRegistrationNumber())
                .capacity(request.getCapacity())
                .foodRequirement(request.getFoodRequirement())
                .acceptedCategories(request.getAcceptedCategories())
                .storageCapacity(request.getStorageCapacity())
                .verificationStatus(userRole == Role.ORGANIZATION ? "pending" : "approved")
                .documentUrl(request.getDocumentUrl())
                .pickupAvailable(request.getPickupAvailable())
                .deliveryRequired(request.getDeliveryRequired())
                .verified(autoVerified)
                .build();

        userRepository.save(user);
        logger.info("New user registered successfully: {} with role {}", user.getEmail(), user.getRole());

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        logger.debug("Login attempt for email: {}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    logger.warn("Login failed: account not found for email: {}", request.getEmail());
                    return new ResourceNotFoundException("No account found with email: " + request.getEmail());
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Login failed: incorrect password for email: {}", request.getEmail());
            throw new IllegalArgumentException("Incorrect password");
        }

        logger.info("User {} logged in successfully", user.getEmail());
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .state(user.getState())
                .district(user.getDistrict())
                .fullAddress(user.getFullAddress())
                .pinCode(user.getPinCode())
                .location(user.getLocation())
                .donationType(user.getDonationType())
                .availability(user.getAvailability())
                .preferredCategories(user.getPreferredCategories())
                .orgName(user.getOrgName())
                .contactPersonName(user.getContactPersonName())
                .orgType(user.getOrgType())
                .registrationNumber(user.getRegistrationNumber())
                .capacity(user.getCapacity())
                .foodRequirement(user.getFoodRequirement())
                .acceptedCategories(user.getAcceptedCategories())
                .storageCapacity(user.getStorageCapacity())
                .verificationStatus(user.getVerificationStatus())
                .documentUrl(user.getDocumentUrl())
                .pickupAvailable(user.getPickupAvailable())
                .deliveryRequired(user.getDeliveryRequired())
                .role(user.getRole())
                .verified(user.isVerified())
                .build();
    }

    private void validateRoleSpecificFields(RegisterRequest request, Role userRole) {
        if (userRole == Role.DONOR) {
            if (request.getName() == null || request.getName().isBlank()) {
                throw new IllegalArgumentException("Full name is required for donors");
            }
            return;
        }

        if (request.getOrgName() == null || request.getOrgName().isBlank()) {
            throw new IllegalArgumentException("Organization name is required");
        }
        if (request.getContactPersonName() == null || request.getContactPersonName().isBlank()) {
            throw new IllegalArgumentException("Contact person name is required");
        }
        if (request.getOrgType() == null || request.getOrgType().isBlank()) {
            throw new IllegalArgumentException("Organization type is required");
        }
        if (request.getRegistrationNumber() == null || request.getRegistrationNumber().isBlank()) {
            throw new IllegalArgumentException("Registration number is required");
        }
        if (request.getDocumentUrl() == null || request.getDocumentUrl().isBlank()) {
            throw new IllegalArgumentException("Verification document is required");
        }
    }
}
