package com.donation.service;

import com.donation.dto.response.MatchResponse;
import com.donation.model.Food;
import com.donation.model.User;
import com.donation.util.LocationUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lightweight matching module providing two capabilities:
 *  1) findBestMatch - ranks verified organizations with strong nearest-distance priority
 *  2) analyzeSuitability - returns human-readable suggestions for the posted donation
 */
@Service
public class AIMatcherService {

    private static final double COST_PER_KM = 10.0;

    public double calculateDistanceKm(String loc1, String loc2) {
        return LocationUtils.calculateDistanceKm(loc1, loc2);
    }

    public double calculateMatchScore(Food food, String targetLocation) {
        String safeTargetLocation = targetLocation != null ? targetLocation : "0,0";
        double distanceKm = calculateDistanceKm(food.getLocation(), safeTargetLocation);
        double distanceScore = priorityDistanceScore(distanceKm);
        double totalScore = (distanceScore * 4.0) + categoryMatchScore(food) + urgencyScore(food);
        return Math.round(totalScore * 100.0) / 100.0;
    }

    private double categoryMatchScore(Food food) {
        if (food.getCategory() == null) return 0.5;
        return switch (food.getCategory()) {
            case VEG, SOUPS, NUTRITIOUS -> 1.0;
            case NON_VEG -> 0.9;
            case SPICY -> 0.75;
            case OTHER -> 0.85;
        };
    }

    private double urgencyScore(Food food) {
        if (food.getExpiry() == null) return 0.3;
        long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), food.getExpiry());
        if (minutesLeft <= 60)  return 1.0;
        if (minutesLeft <= 180) return 0.7;
        if (minutesLeft <= 360) return 0.4;
        return 0.2;
    }

    public MatchResponse findBestMatch(Food food, List<User> organizations) {
        if (organizations == null || organizations.isEmpty()) {
            return null;
        }

        return organizations.stream()
                .map(org -> {
                    String orgLoc = org.getLocation() != null ? org.getLocation() : "0,0";
                    double distanceKm = calculateDistanceKm(food.getLocation(), orgLoc);
                    double totalScore = calculateMatchScore(food, orgLoc);
                    double cost = Math.round(distanceKm * COST_PER_KM * 100.0) / 100.0;

                    return MatchResponse.builder()
                            .organizationId(org.getId())
                            .organizationName(org.getOrgName() != null && !org.getOrgName().isBlank() ? org.getOrgName() : org.getName())
                            .location(orgLoc)
                            .score(Math.round(totalScore * 100.0) / 100.0)
                            .distanceKm(Math.round(distanceKm * 100.0) / 100.0)
                            .estimatedDeliveryCost(cost)
                            .build();
                })
                .max(Comparator.comparingDouble(MatchResponse::getScore))
                .orElse(null);
    }

    private double priorityDistanceScore(double distanceKm) {
        if (distanceKm <= 1.0) return 8.0;
        if (distanceKm <= 3.0) return 5.5;
        if (distanceKm <= 8.0) return 3.4;
        if (distanceKm <= 15.0) return 1.9;
        if (distanceKm <= 25.0) return 0.9;
        return 0.35;
    }

    public List<String> analyzeSuitability(Food food) {
        List<String> suggestions = new ArrayList<>();
        if (food == null || food.getCategory() == null) return suggestions;

        switch (food.getCategory()) {
            case VEG -> {
                suggestions.add("Mention allergens and dietary notes (oil, dairy, gluten).");
                suggestions.add("Share packaging + storage guidance and expiry time.");
            }
            case NON_VEG -> {
                suggestions.add("Clearly mention the meat/ingredients and cooking time.");
                suggestions.add("Use sealed containers and share expiry time for safety.");
            }
            case SOUPS -> {
                suggestions.add("Indicate if it needs reheating and how long it stays safe.");
                suggestions.add("Use spill-proof containers and share pickup instructions.");
            }
            case NUTRITIOUS -> {
                suggestions.add("Mention nutrition focus (high-protein, low-sugar, child-friendly).");
                suggestions.add("Share packaging + storage guidance and expiry time.");
            }
            case SPICY -> {
                suggestions.add("Mention spice level and offer a milder option if possible.");
                suggestions.add("Clearly note if it may not suit children/elderly.");
            }
            case OTHER -> {
                suggestions.add("Add a clear description so organizations can judge fit quickly.");
                suggestions.add("Mention packaging, ingredients, and any handling notes.");
            }
        }
        return suggestions;
    }
}
