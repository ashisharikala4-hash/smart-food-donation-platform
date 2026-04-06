package com.donation.service;

import com.donation.dto.request.DeliveryUpdateRequest;
import com.donation.dto.response.DeliveryResponse;

public interface DeliveryService {
    DeliveryResponse createDelivery(Long foodId);
    DeliveryResponse updateDeliveryStatus(Long deliveryId, DeliveryUpdateRequest request);
    DeliveryResponse getDeliveryByFoodId(Long foodId);
    DeliveryResponse assignNgo(Long foodId, Long ngoId);
    DeliveryResponse assignNearestNgo(Long foodId);
}
