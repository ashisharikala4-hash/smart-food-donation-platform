package com.donation.service;

import com.donation.dto.request.FoodRequest;
import com.donation.dto.response.FoodResponse;
import com.donation.dto.response.MatchResponse;
import com.donation.enums.DeliveryMode;

import java.util.List;

public interface FoodService {
    FoodResponse addFood(FoodRequest request, String donorEmail);
    List<FoodResponse> getAllFood();
    List<FoodResponse> getDonorFood(String donorEmail);
    FoodResponse acceptFood(Long foodId, String orgEmail);
    FoodResponse rejectFood(Long foodId, String orgEmail);
    FoodResponse updateStatus(Long foodId, String status, String userEmail);
    void deleteFood(Long foodId, String donorEmail);
    FoodResponse selectDeliveryMode(Long foodId, DeliveryMode mode, String userEmail);
    MatchResponse getBestMatch(Long foodId);
    List<FoodResponse> getRankedFood(String orgEmail);
}
