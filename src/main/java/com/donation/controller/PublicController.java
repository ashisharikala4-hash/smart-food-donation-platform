package com.donation.controller;

import com.donation.dto.response.PublicMetricsResponse;
import com.donation.enums.FoodStatus;
import com.donation.enums.Role;
import com.donation.repository.FoodRepository;
import com.donation.repository.NgoRepository;
import com.donation.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final NgoRepository ngoRepository;

    public PublicController(FoodRepository foodRepository, UserRepository userRepository, NgoRepository ngoRepository) {
        this.foodRepository = foodRepository;
        this.userRepository = userRepository;
        this.ngoRepository = ngoRepository;
    }

    @GetMapping("/metrics")
    public ResponseEntity<PublicMetricsResponse> getMetrics() {
        long totalDonations = foodRepository.count();
        long deliveredDonations = foodRepository.findByStatus(FoodStatus.DELIVERED).size();
        long activeDonations = Math.max(0, totalDonations - deliveredDonations);
        long verifiedOrganizations = userRepository.findByRoleAndVerified(Role.ORGANIZATION, true).size();
        long availableNgos = ngoRepository.findByAvailableTrue().size();

        return ResponseEntity.ok(PublicMetricsResponse.builder()
                .totalDonations(totalDonations)
                .activeDonations(activeDonations)
                .deliveredDonations(deliveredDonations)
                .verifiedOrganizations(verifiedOrganizations)
                .availableNgos(availableNgos)
                .build());
    }
}

