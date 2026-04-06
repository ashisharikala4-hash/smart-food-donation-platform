package com.donation.service.impl;

import com.donation.dto.request.DeliveryUpdateRequest;
import com.donation.dto.response.DeliveryResponse;
import com.donation.enums.DeliveryMode;
import com.donation.enums.FoodStatus;
import com.donation.exception.ResourceNotFoundException;
import com.donation.model.Delivery;
import com.donation.model.Food;
import com.donation.model.Ngo;
import com.donation.repository.DeliveryRepository;
import com.donation.repository.FoodRepository;
import com.donation.repository.NgoRepository;
import com.donation.service.AIMatcherService;
import com.donation.service.ChatService;
import com.donation.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class DeliveryServiceImpl implements DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryServiceImpl.class);

    private final DeliveryRepository deliveryRepository;
    private final FoodRepository foodRepository;
    private final NgoRepository ngoRepository;
    private final AIMatcherService aiMatcherService;
    private final ChatService chatService;

    public DeliveryServiceImpl(DeliveryRepository deliveryRepository, FoodRepository foodRepository,
                               NgoRepository ngoRepository, AIMatcherService aiMatcherService,
                               @Lazy ChatService chatService) {
        this.deliveryRepository = deliveryRepository;
        this.foodRepository = foodRepository;
        this.ngoRepository = ngoRepository;
        this.aiMatcherService = aiMatcherService;
        this.chatService = chatService;
    }

    @Override
    public DeliveryResponse createDelivery(Long foodId) {
        logger.info("Creating delivery record for food listing: {}", foodId);
        Food food = findFoodById(foodId);

        deliveryRepository.findByFood(food).ifPresent(existing -> {
            logger.warn("Delivery record already exists for food: {}", foodId);
            throw new IllegalArgumentException("A delivery record already exists for food listing: " + foodId);
        });

        String orgLocation = (food.getAcceptedBy() != null && food.getAcceptedBy().getLocation() != null)
                ? food.getAcceptedBy().getLocation()
                : "0,0";

        double distanceKm = aiMatcherService.calculateDistanceKm(food.getLocation(), orgLocation);
        double estimatedCost = Math.round(distanceKm * 10.0 * 100.0) / 100.0;

        Delivery delivery = Delivery.builder()
                .food(food)
                .deliveryType(food.getDeliveryMode())
                .status(food.getStatus())
                .estimatedCost(estimatedCost)
                .build();

        deliveryRepository.save(delivery);
        logger.info("Delivery record created successfully for food: {}", foodId);
        return toResponse(delivery);
    }

    @Override
    @Transactional
    public DeliveryResponse updateDeliveryStatus(Long deliveryId, DeliveryUpdateRequest request) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found: " + deliveryId));

        logger.info("Updating delivery {} status to {}", deliveryId, request.getStatus());
        delivery.setStatus(request.getStatus());
        delivery.getFood().setStatus(request.getStatus());

        foodRepository.save(delivery.getFood());
        deliveryRepository.save(delivery);

        return toResponse(delivery);
    }

    @Override
    public DeliveryResponse getDeliveryByFoodId(Long foodId) {
        Food food = findFoodById(foodId);
        return deliveryRepository.findByFood(food)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No delivery found for food listing: " + foodId));
    }

    @Override
    @Transactional
    public DeliveryResponse assignNgo(Long foodId, Long ngoId) {
        Food food = findFoodById(foodId);

        if (!requiresPartnerAssignment(food.getDeliveryMode())) {
            throw new IllegalArgumentException("A partner can only be assigned when the logistics path requires assisted delivery");
        }

        if (food.getDeliveryMode() != DeliveryMode.NGO_DELIVERY && food.getDeliveryMode() != DeliveryMode.DELIVERY_PARTNER) {
            throw new IllegalArgumentException("This request does not require NGO-assisted delivery");
        }

        Ngo ngo = ngoRepository.findById(ngoId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO not found: " + ngoId));

        Delivery delivery = deliveryRepository.findByFood(food)
                .orElseGet(() -> buildDelivery(food));

        food.setStatus(FoodStatus.PENDING_NGO);
        foodRepository.save(food);

        delivery.setDeliveryType(food.getDeliveryMode());
        delivery.setStatus(food.getStatus());
        delivery.setAssignedNgo(ngo);
        deliveryRepository.save(delivery);
        logger.info("NGO {} assigned to delivery for food {}", ngo.getName(), foodId);
        
        chatService.sendSystemMessage(foodId, buildPartnerAssignedMessage(food.getDeliveryMode(), ngo));
        
        return toResponse(delivery);
    }

    @Override
    @Transactional
    public DeliveryResponse assignNearestNgo(Long foodId) {
        Food food = findFoodById(foodId);
        List<Ngo> availableNgos = ngoRepository.findByAvailableTrue();

        if (availableNgos.isEmpty()) {
            logger.warn("No available NGOs found for auto-assignment of food {}", foodId);
            food.setStatus(com.donation.enums.FoodStatus.PENDING_NGO);
            foodRepository.save(food);
            
            // Create dummy delivery record if not exists to track status
            updateOrCreateDeliveryRecord(food);
            
            chatService.sendSystemMessage(foodId, "Waiting for partner assignment. No partner organizations are currently available in your area.");
            return getDeliveryByFoodId(foodId);
        }

        // Find closest NGO
        Ngo nearest = availableNgos.stream()
                .min(Comparator.comparingDouble(ngo -> 
                    aiMatcherService.calculateDistanceKm(food.getLocation(), ngo.getLocation())))
                .orElse(null);

        if (nearest != null) {
            return assignNgo(foodId, nearest.getId());
        }

        return getDeliveryByFoodId(foodId);
    }

    private void updateOrCreateDeliveryRecord(Food food) {
        if (deliveryRepository.findByFood(food).isEmpty()) {
            deliveryRepository.save(buildDelivery(food));
        } else {
            Delivery delivery = deliveryRepository.findByFood(food).get();
            delivery.setDeliveryType(food.getDeliveryMode());
            delivery.setStatus(food.getStatus());
            deliveryRepository.save(delivery);
        }
    }

    private Delivery buildDelivery(Food food) {
        String orgLocation = (food.getAcceptedBy() != null && food.getAcceptedBy().getLocation() != null)
                ? food.getAcceptedBy().getLocation() : "0,0";
        double distanceKm = aiMatcherService.calculateDistanceKm(food.getLocation(), orgLocation);
        double estimatedCost = Math.round(distanceKm * 10.0 * 100.0) / 100.0;

        return Delivery.builder()
                .food(food)
                .deliveryType(food.getDeliveryMode())
                .status(food.getStatus())
                .estimatedCost(estimatedCost)
                .build();
    }

    private boolean requiresPartnerAssignment(DeliveryMode mode) {
        return mode == DeliveryMode.NGO_DELIVERY || mode == DeliveryMode.DELIVERY_PARTNER;
    }

    private String buildPartnerAssignedMessage(DeliveryMode mode, Ngo ngo) {
        String partnerName = ngo.getName();
        if (mode == DeliveryMode.DELIVERY_PARTNER) {
            return "Delivery partner " + partnerName + " selected. Pickup can begin once the donor confirms handoff.";
        }
        String contact = ngo.getContact() != null && !ngo.getContact().isBlank() ? ngo.getContact() : "contact unavailable";
        return "Nearest NGO selected: " + partnerName + " (" + contact + "). Pickup can begin once the donor confirms handoff.";
    }

    private Food findFoodById(Long foodId) {
        return foodRepository.findById(foodId)
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found: " + foodId));
    }

    private DeliveryResponse toResponse(Delivery delivery) {
        return DeliveryResponse.builder()
                .id(delivery.getId())
                .foodId(delivery.getFood().getId())
                .deliveryType(delivery.getDeliveryType())
                .status(delivery.getStatus())
                .estimatedCost(delivery.getEstimatedCost())
                .ngoId(delivery.getAssignedNgo() != null ? delivery.getAssignedNgo().getId() : null)
                .ngoName(delivery.getAssignedNgo() != null ? delivery.getAssignedNgo().getName() : null)
                .ngoContact(delivery.getAssignedNgo() != null ? delivery.getAssignedNgo().getContact() : null)
                .ngoLocation(delivery.getAssignedNgo() != null ? delivery.getAssignedNgo().getLocation() : null)
                .build();
    }
}
