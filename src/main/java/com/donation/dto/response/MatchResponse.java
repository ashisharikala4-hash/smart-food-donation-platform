package com.donation.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MatchResponse {
    private Long organizationId;
    private String organizationName;
    private String location;
    private double score;
    private double distanceKm;
    private double estimatedDeliveryCost;
}
