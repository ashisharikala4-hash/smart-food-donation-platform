package com.donation.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicMetricsResponse {
    private long totalDonations;
    private long activeDonations;
    private long deliveredDonations;
    private long verifiedOrganizations;
    private long availableNgos;
}

