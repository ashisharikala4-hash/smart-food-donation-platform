package com.donation.controller;

import com.donation.dto.response.UserResponse;
import com.donation.enums.Role;
import com.donation.exception.ResourceNotFoundException;
import com.donation.model.User;
import com.donation.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/organizations")
    public ResponseEntity<List<UserResponse>> getAllOrganizations() {
        List<UserResponse> orgs = userRepository.findByRole(Role.ORGANIZATION)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(orgs);
    }

    @PatchMapping("/verify/{userId}")
    public ResponseEntity<UserResponse> verifyOrganization(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getRole() != Role.ORGANIZATION) {
            throw new IllegalArgumentException("Only organization accounts can be verified");
        }

        user.setVerified(true);
        user.setVerificationStatus("approved");
        userRepository.save(user);
        return ResponseEntity.ok(toResponse(user));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalOrgs = userRepository.findByRole(Role.ORGANIZATION).size();
        long verifiedOrgs = userRepository.findByRoleAndVerified(Role.ORGANIZATION, true).size();
        long pendingOrgs = totalOrgs - verifiedOrgs;
        return ResponseEntity.ok(java.util.Map.of(
                "totalOrganizations", totalOrgs,
                "verifiedOrganizations", verifiedOrgs,
                "pendingVerification", pendingOrgs
        ));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .role(user.getRole())
                .orgName(user.getOrgName())
                .contactPersonName(user.getContactPersonName())
                .orgType(user.getOrgType())
                .registrationNumber(user.getRegistrationNumber())
                .location(user.getLocation())
                .fullAddress(user.getFullAddress())
                .pickupAvailable(user.getPickupAvailable())
                .deliveryRequired(user.getDeliveryRequired())
                .verificationStatus(user.getVerificationStatus())
                .verified(user.isVerified())
                .build();
    }
}