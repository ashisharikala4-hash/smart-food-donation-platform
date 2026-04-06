package com.donation.service.impl;

import com.donation.dto.request.FoodRequest;
import com.donation.dto.response.FoodResponse;
import com.donation.dto.response.MatchResponse;
import com.donation.enums.DeliveryMode;
import com.donation.enums.DonationCategory;
import com.donation.enums.DonationCondition;
import com.donation.enums.FoodStatus;
import com.donation.enums.Role;
import com.donation.exception.ResourceNotFoundException;
import com.donation.exception.UnauthorizedException;
import com.donation.model.Food;
import com.donation.model.User;
import com.donation.repository.DeliveryRepository;
import com.donation.repository.FoodRepository;
import com.donation.repository.MessageRepository;
import com.donation.repository.UserRepository;
import com.donation.model.Delivery;
import com.donation.service.AIMatcherService;
import com.donation.service.ChatService;

import com.donation.service.FoodService;
import com.donation.model.Ngo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FoodServiceImpl implements FoodService {

    private static final Logger logger = LoggerFactory.getLogger(FoodServiceImpl.class);

    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final DeliveryRepository deliveryRepository;
    private final MessageRepository messageRepository;
    private final AIMatcherService aiMatcherService;
    private final ChatService chatService;


    public FoodServiceImpl(FoodRepository foodRepository, UserRepository userRepository, 
                           DeliveryRepository deliveryRepository, MessageRepository messageRepository,
                           AIMatcherService aiMatcherService,
                           @Lazy ChatService chatService) {
        this.foodRepository = foodRepository;
        this.userRepository = userRepository;
        this.deliveryRepository = deliveryRepository;
        this.messageRepository = messageRepository;
        this.aiMatcherService = aiMatcherService;
        this.chatService = chatService;
    }

    @Override
    public FoodResponse addFood(FoodRequest request, String donorEmail) {
        User donor = findUserByEmail(donorEmail);
        logger.info("Adding new donation for donor: {}", donorEmail);
        validateFoodRequest(request);

        DonationCategory category;
        try {
            category = DonationCategory.valueOf(request.getCategory().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Invalid category value: " + request.getCategory());
        }

        DonationCondition condition = null;
        if (request.getCondition() != null && !request.getCondition().isBlank()) {
            try {
                condition = DonationCondition.valueOf(request.getCondition().toUpperCase());
            } catch (Exception e) {
                throw new RuntimeException("Invalid condition value: " + request.getCondition());
            }
        }

        DeliveryMode donorDeliveryPreference = parseDeliveryPreference(request.getDonorDeliveryPreference());

        Food food = Food.builder()
                .donor(donor)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(category)
                .itemType(request.getType())
                .quantity(request.getQuantity())
                .condition(condition)
                .expiry(request.getExpiry())
                .images(request.getImages())
                .location(request.getLocation())
                .deliveryMode(null)
                .donorDeliveryPreference(donorDeliveryPreference)
                .status(FoodStatus.POSTED)
                .createdAt(LocalDateTime.now())
                .build();

        foodRepository.save(food);
        logger.info("Donation saved successfully with ID: {}", food.getId());
        return toResponse(food);
    }

    @Override
    public List<FoodResponse> getAllFood() {
        return foodRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<FoodResponse> getDonorFood(String donorEmail) {
        User donor = findUserByEmail(donorEmail);
        return foodRepository.findByDonorOrderByExpiryAsc(donor).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public FoodResponse acceptFood(Long foodId, String orgEmail) {
        User org = findUserByEmail(orgEmail);
        logger.info("Organization {} attempting to accept food {}", orgEmail, foodId);

        if (!org.isVerified()) {
            throw new UnauthorizedException("Your organization has not been verified by admin yet");
        }

        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found: " + foodId));

        if (food.getStatus() != FoodStatus.POSTED) {
            throw new IllegalArgumentException("This listing has already been accepted or is unavailable");
        }

        food.getDeclinedByOrganizations().remove(org);
        food.setStatus(FoodStatus.ACCEPTED);
        food.setAcceptedBy(org);
        food.setDeliveryMode(null);
        food.setDonorDeliveryPreference(null);
        food.setOrganizationDeliveryPreference(null);
        foodRepository.save(food);

        chatService.sendSystemMessage(foodId, "Donation accepted by " + org.getName());
        chatService.sendSystemMessage(foodId,
                "Contacts unlocked. Donor: " + buildContactLabel(food.getDonor()) + ". Organization: " + buildContactLabel(org) + ".");
        chatService.sendSystemMessage(foodId,
                "The donor can now review the organization distance, address, and pickup support before confirming the delivery plan.");
        chatService.sendSystemMessage(foodId,
                "Next step: donor chooses either direct delivery or pickup support. The organization responds after the donor confirms.");

        return toResponse(food);
    }

    @Override
    @Transactional
    public FoodResponse rejectFood(Long foodId, String orgEmail) {
        User org = findUserByEmail(orgEmail);
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found: " + foodId));

        if (food.getStatus() == FoodStatus.POSTED) {
            food.getDeclinedByOrganizations().add(org);
            foodRepository.save(food);
            return toResponse(food);
        }

        boolean isAcceptedOrg = food.getAcceptedBy() != null && food.getAcceptedBy().getEmail().equals(orgEmail);
        if (!isAcceptedOrg) {
            throw new UnauthorizedException("Only the assigned organization can reject this donation");
        }

        if (food.getStatus() == FoodStatus.PICKUP_STARTED || food.getStatus() == FoodStatus.DELIVERED) {
            throw new IllegalArgumentException("This donation cannot be rejected after pickup has started");
        }

        food.getDeclinedByOrganizations().add(org);
        resetDeliveryWorkflow(food, true);
        foodRepository.save(food);
        return toResponse(food);
    }

    @Override
    @Transactional
    public FoodResponse selectDeliveryMode(Long foodId, DeliveryMode mode, String userEmail) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found: " + foodId));

        boolean isDonor = food.getDonor().getEmail().equals(userEmail);
        boolean isAcceptedOrganization = food.getAcceptedBy() != null
                && food.getAcceptedBy().getEmail().equals(userEmail);

        if (!isDonor && !isAcceptedOrganization) {
            throw new UnauthorizedException("You are not authorized to select delivery mode for this item");
        }

        if (food.getStatus() == FoodStatus.PICKUP_STARTED || food.getStatus() == FoodStatus.DELIVERED) {
            throw new UnauthorizedException("Delivery coordination cannot be changed after pickup starts");
        }
        if (food.getStatus() != FoodStatus.ACCEPTED && food.getStatus() != FoodStatus.PENDING_NGO && food.getStatus() != FoodStatus.POSTED) {
            throw new UnauthorizedException("Delivery coordination is only available before pickup starts");
        }

        validateRoleBasedDeliverySelection(food, isDonor, mode);

        logger.info("User {} submitted delivery preference {} for food {}", userEmail, mode, foodId);
        if (isDonor) {
            DeliveryMode previousDonorMode = food.getDonorDeliveryPreference();
            food.setDonorDeliveryPreference(mode);
            if (previousDonorMode != null && previousDonorMode != mode) {
                food.setOrganizationDeliveryPreference(null);
                food.setDeliveryMode(null);
                clearPartnerAssignment(food);
                chatService.sendSystemMessage(foodId,
                        "Donor updated the delivery choice to " + formatDeliveryMode(mode) + ". Organization confirmation is needed again.");
            } else {
                chatService.sendSystemMessage(foodId, "Donor preference updated: " + formatDeliveryMode(mode));
            }
        } else {
            food.setOrganizationDeliveryPreference(mode);
            chatService.sendSystemMessage(foodId, "Organization preference updated: " + formatDeliveryMode(mode));
        }

        DeliveryMode resolvedMode = resolveDeliveryMode(food);
        if (resolvedMode != null) {
            food.setDeliveryMode(resolvedMode);
            if (requiresPartnerAssignment(food.getDeliveryMode())) {
                food.setStatus(FoodStatus.PENDING_NGO);
                chatService.sendSystemMessage(foodId, buildPartnerAgreementMessage(food.getDeliveryMode()));
            } else {
                food.setStatus(FoodStatus.ACCEPTED);
                chatService.sendSystemMessage(foodId, "Both sides confirmed: " + formatDeliveryMode(food.getDeliveryMode()));
            }

            updateOrCreateDeliveryRecord(food, food.getDeliveryMode());
            logger.info("Delivery agreement reached for food {} with mode {}", foodId, food.getDeliveryMode());
        } else {
            food.setDeliveryMode(null);
            food.setStatus(food.getAcceptedBy() == null ? FoodStatus.POSTED : FoodStatus.ACCEPTED);
            clearPartnerAssignment(food);
            chatService.sendSystemMessage(foodId, buildPendingAgreementMessage(food));
        }

        foodRepository.save(food);
        return toResponse(food);
    }

    private Delivery updateOrCreateDeliveryRecord(Food food, DeliveryMode mode) {
        String orgLocation = (food.getAcceptedBy() != null && food.getAcceptedBy().getLocation() != null)
                ? food.getAcceptedBy().getLocation() : "0,0";
        double distanceKm = aiMatcherService.calculateDistanceKm(food.getLocation(), orgLocation);
        double estimatedCost = Math.round(distanceKm * 10.0 * 100.0) / 100.0;

        Delivery delivery = deliveryRepository.findByFood(food).orElseGet(() -> 
            Delivery.builder().food(food).build()
        );
        
        delivery.setDeliveryType(mode);
        delivery.setStatus(food.getStatus());
        delivery.setEstimatedCost(estimatedCost);
        if (!requiresPartnerAssignment(mode)) {
            delivery.setAssignedNgo(null);
        }
        
        return deliveryRepository.save(delivery);
    }

    @Override
    @Transactional
    public FoodResponse updateStatus(Long foodId, String status, String userEmail) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found: " + foodId));

        boolean isDonor = food.getDonor().getEmail().equals(userEmail);
        boolean isAcceptedOrg = food.getAcceptedBy() != null && food.getAcceptedBy().getEmail().equals(userEmail);
        User actor = findUserByEmail(userEmail);
        boolean isAdmin = actor.getRole() == Role.ADMIN;
        if (!isDonor && !isAcceptedOrg && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to update this donation");
        }

        FoodStatus newStatus;
        try {
            newStatus = FoodStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value: " + status);
        }

        validateStatusActor(newStatus, isDonor, isAcceptedOrg, isAdmin);
        validateStatusTransition(food, newStatus);
        logger.info("Updating status of food {} to {}", foodId, newStatus);
        food.setStatus(newStatus);

        foodRepository.save(food);
        
        // Sync with Delivery status
        deliveryRepository.findByFood(food).ifPresent(delivery -> {
            delivery.setStatus(food.getStatus());
            deliveryRepository.save(delivery);
        });

        if (newStatus == FoodStatus.PICKUP_STARTED) {
            chatService.sendSystemMessage(foodId, "Pickup confirmed by donor. The organization can mark this donation delivered after handoff.");
        } else if (newStatus == FoodStatus.DELIVERED) {
            String donorName = food.getDonor().getName();
            String organizationName = food.getAcceptedBy() != null
                    ? (food.getAcceptedBy().getOrgName() != null && !food.getAcceptedBy().getOrgName().isBlank()
                    ? food.getAcceptedBy().getOrgName()
                    : food.getAcceptedBy().getName())
                    : "the organization";
            chatService.sendSystemMessage(
                    foodId,
                    "Delivery confirmed. Thank you for your donation, " + donorName + ". Thank you as well, "
                            + organizationName + ", for completing the handoff. The conversation stays open for final thanks and follow-up notes."
            );
        }

        return toResponse(food);
    }

    @Override
    @Transactional
    public void deleteFood(Long foodId, String donorEmail) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found: " + foodId));

        if (!food.getDonor().getEmail().equals(donorEmail)) {
            throw new UnauthorizedException("You can only delete your own donation");
        }

        if (food.getStatus() != FoodStatus.POSTED) {
            throw new IllegalArgumentException("Only unaccepted posted donations can be deleted");
        }

        deleteWorkflowArtifacts(food);
        food.getDeclinedByOrganizations().clear();
        foodRepository.delete(food);
    }

    @Override
    public MatchResponse getBestMatch(Long foodId) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found: " + foodId));

        List<User> verifiedOrgs = userRepository.findByRoleAndVerified(Role.ORGANIZATION, true);
        return aiMatcherService.findBestMatch(food, verifiedOrgs);
    }

    @Override
    public List<FoodResponse> getRankedFood(String orgEmail) {
        User org = findUserByEmail(orgEmail);
        String orgLocation = org.getLocation() != null ? org.getLocation() : "0,0";
        List<String> acceptedCategories = parseCsv(org.getAcceptedCategories());

        List<Food> postedFood = foodRepository.findAll().stream()
                .filter(f -> f.getStatus() == FoodStatus.POSTED)
                .filter(f -> f.getDeclinedByOrganizations().stream().noneMatch(user -> user.getId().equals(org.getId())))
                .filter(f -> acceptedCategories.isEmpty() || acceptedCategories.contains(f.getCategory().name()))
                .toList();

        if (postedFood.isEmpty()) {
            return List.of();
        }

        // Calculate AI score for each food relative to this organization
        List<FoodResponse> ranked = postedFood.stream()
                .map(food -> {
                    double totalScore = aiMatcherService.calculateMatchScore(food, orgLocation);

                    FoodResponse resp = toResponse(food);
                    resp.setAiScore(totalScore);
                    return resp;
                })
                .sorted((a, b) -> Double.compare(b.getAiScore(), a.getAiScore()))
                .toList();

        // Mark the top item as recommended
        if (!ranked.isEmpty()) {
            ranked.get(0).setRecommended(true);
        }

        return ranked;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void validateFoodRequest(FoodRequest request) {
        if (request.getQuantity() < 1) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Donation title is required");
        }

        if (request.getExpiry() != null && !request.getExpiry().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Expiry time must be in the future");
        }
    }

    private DeliveryMode parseDeliveryPreference(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            DeliveryMode mode = DeliveryMode.valueOf(rawValue.trim().toUpperCase());
            if (mode != DeliveryMode.DONOR_DELIVERY && mode != DeliveryMode.ORG_PICKUP) {
                throw new IllegalArgumentException("Invalid donor delivery preference: " + rawValue);
            }
            return mode;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid donor delivery preference: " + rawValue);
        }
    }

    private List<String> parseCsv(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return List.of();
        return java.util.Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toUpperCase)
                .toList();
    }

    private void validateRoleBasedDeliverySelection(Food food, boolean isDonor, DeliveryMode mode) {
        if (isDonor) {
            if (mode != DeliveryMode.DONOR_DELIVERY && mode != DeliveryMode.ORG_PICKUP) {
                throw new IllegalArgumentException("Donors can only choose donor delivery or organization pickup");
            }
            return;
        }

        DeliveryMode donorMode = food.getDonorDeliveryPreference();
        if (donorMode == null) {
            throw new IllegalArgumentException("The organization must wait for the donor preference before choosing a delivery plan");
        }

        if (donorMode == DeliveryMode.DONOR_DELIVERY && mode != DeliveryMode.DONOR_DELIVERY) {
            throw new IllegalArgumentException("When the donor is ready to deliver, the organization can only confirm donor delivery");
        }

        if (donorMode == DeliveryMode.ORG_PICKUP
                && mode != DeliveryMode.ORG_PICKUP
                && mode != DeliveryMode.NGO_DELIVERY) {
            throw new IllegalArgumentException("When the donor needs pickup, the organization can only choose pickup or nearby NGO help");
        }
    }

    private DeliveryMode resolveDeliveryMode(Food food) {
        DeliveryMode donorMode = food.getDonorDeliveryPreference();
        DeliveryMode organizationMode = food.getOrganizationDeliveryPreference();

        if (donorMode == null || organizationMode == null) {
            return null;
        }

        if (donorMode == DeliveryMode.DONOR_DELIVERY) {
            return organizationMode == DeliveryMode.DONOR_DELIVERY ? DeliveryMode.DONOR_DELIVERY : null;
        }

        if (donorMode == DeliveryMode.ORG_PICKUP) {
            if (organizationMode == DeliveryMode.ORG_PICKUP
                    || organizationMode == DeliveryMode.NGO_DELIVERY) {
                return organizationMode;
            }
            return null;
        }

        return donorMode == organizationMode ? donorMode : null;
    }

    private void clearPartnerAssignment(Food food) {
        deliveryRepository.findByFood(food).ifPresent(delivery -> {
            delivery.setAssignedNgo(null);
            delivery.setDeliveryType(food.getDeliveryMode());
            delivery.setStatus(food.getStatus());
            deliveryRepository.save(delivery);
        });
    }

    private void resetDeliveryWorkflow(Food food, boolean clearAcceptedOrg) {
        food.setStatus(FoodStatus.POSTED);
        food.setDeliveryMode(null);
        food.setDonorDeliveryPreference(null);
        food.setOrganizationDeliveryPreference(null);
        if (clearAcceptedOrg) {
            food.setAcceptedBy(null);
        }
        deleteWorkflowArtifacts(food);
    }

    private void deleteWorkflowArtifacts(Food food) {
        messageRepository.deleteByFood(food);
        deliveryRepository.deleteByFood(food);
    }

    private void validateStatusActor(FoodStatus newStatus, boolean isDonor, boolean isAcceptedOrg, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (newStatus == FoodStatus.PICKUP_STARTED && !isDonor) {
            throw new UnauthorizedException("Only the donor can confirm that the order has been picked up");
        }

        if (newStatus == FoodStatus.DELIVERED && !isAcceptedOrg) {
            throw new UnauthorizedException("Only the organization can confirm that the order has been delivered");
        }
    }

    private FoodResponse toResponse(Food food) {
        List<String> suitability = aiMatcherService.analyzeSuitability(food);
        String ngoName = deliveryRepository.findByFood(food)
                .map(Delivery::getAssignedNgo)
                .map(Ngo::getName)
                .orElse(null);
        boolean deliveryAgreementReached = food.getDeliveryMode() != null;
        return FoodResponse.builder()
                .id(food.getId())
                .donorId(food.getDonor().getId())
                .donorName(food.getDonor().getName())
                .donorContact(food.getDonor().getMobileNumber() != null && !food.getDonor().getMobileNumber().isBlank()
                        ? food.getDonor().getMobileNumber()
                        : food.getDonor().getEmail())
                .title(food.getTitle())
                .description(food.getDescription())
                .category(food.getCategory())
                .foodType(food.getCategory())
                .type(food.getItemType())
                .quantity(food.getQuantity())
                .condition(food.getCondition())
                .expiryTime(food.getExpiry())
                .images(food.getImages())
                .location(food.getLocation())
                .deliveryMode(food.getDeliveryMode())
                .donorDeliveryPreference(food.getDonorDeliveryPreference())
                .organizationDeliveryPreference(food.getOrganizationDeliveryPreference())
                .deliveryAgreementReached(deliveryAgreementReached)
                .status(food.getStatus())
                .acceptedById(food.getAcceptedBy() != null ? food.getAcceptedBy().getId() : null)
                .acceptedByName(food.getAcceptedBy() != null ? (food.getAcceptedBy().getOrgName() != null && !food.getAcceptedBy().getOrgName().isBlank() ? food.getAcceptedBy().getOrgName() : food.getAcceptedBy().getName()) : null)
                .acceptedByContact(food.getAcceptedBy() != null
                        ? (food.getAcceptedBy().getMobileNumber() != null && !food.getAcceptedBy().getMobileNumber().isBlank()
                        ? food.getAcceptedBy().getMobileNumber()
                        : food.getAcceptedBy().getEmail())
                        : null)
                .acceptedByLocation(food.getAcceptedBy() != null ? food.getAcceptedBy().getLocation() : null)
                .acceptedByAddress(food.getAcceptedBy() != null ? food.getAcceptedBy().getFullAddress() : null)
                .acceptedByPickupAvailable(food.getAcceptedBy() != null ? food.getAcceptedBy().getPickupAvailable() : null)
                .organizationName(food.getAcceptedBy() != null ? (food.getAcceptedBy().getOrgName() != null && !food.getAcceptedBy().getOrgName().isBlank() ? food.getAcceptedBy().getOrgName() : food.getAcceptedBy().getName()) : null)
                .ngoName(ngoName)
                .createdAt(food.getCreatedAt())
                .suitability(suitability)
                .build();
    }

    private void validateStatusTransition(Food food, FoodStatus newStatus) {
        if (newStatus == FoodStatus.PICKUP_STARTED) {
            if (food.getDeliveryMode() == null) {
                throw new IllegalArgumentException("Both sides must agree on a delivery path before pickup can start");
            }

            if (requiresPartnerAssignment(food.getDeliveryMode())
                    && deliveryRepository.findByFood(food).map(Delivery::getAssignedNgo).isEmpty()) {
                throw new IllegalArgumentException(buildPartnerAssignmentRequiredMessage(food.getDeliveryMode()));
            }
        }

        if (newStatus == FoodStatus.DELIVERED && food.getStatus() != FoodStatus.PICKUP_STARTED) {
            throw new IllegalArgumentException("The donation can only be marked delivered after pickup has started");
        }
    }

    private String buildPendingAgreementMessage(Food food) {
        DeliveryMode donorChoice = food.getDonorDeliveryPreference();
        DeliveryMode organizationChoice = food.getOrganizationDeliveryPreference();

        if (donorChoice == null) {
            return "Waiting for the donor to confirm the delivery plan after reviewing the organization details.";
        }

        if (donorChoice == DeliveryMode.DONOR_DELIVERY && organizationChoice == null) {
            return "Donor is ready to deliver. Waiting for the organization to confirm donor delivery.";
        }

        if (donorChoice == DeliveryMode.ORG_PICKUP && organizationChoice == null) {
            return "Donor needs pickup. Waiting for the organization to choose pickup or nearby NGO help.";
        }

        return "Delivery agreement pending. Donor: "
                + Optional.ofNullable(donorChoice).map(this::formatDeliveryMode).orElse("waiting for donor")
                + ". Organization: "
                + Optional.ofNullable(organizationChoice).map(this::formatDeliveryMode).orElse("waiting for organization")
                + ".";
    }

    private String formatDeliveryMode(DeliveryMode mode) {
        return mode == null ? "Not selected" : mode.name().replace('_', ' ');
    }

    private String buildContactLabel(User user) {
        if (user == null) return "Not available";
        String phoneOrEmail = user.getMobileNumber() != null && !user.getMobileNumber().isBlank()
                ? user.getMobileNumber()
                : user.getEmail();
        String displayName = user.getOrgName() != null && !user.getOrgName().isBlank()
                ? user.getOrgName()
                : user.getName();
        return displayName + " (" + phoneOrEmail + ")";
    }

    private boolean requiresPartnerAssignment(DeliveryMode mode) {
        return mode == DeliveryMode.NGO_DELIVERY || mode == DeliveryMode.DELIVERY_PARTNER;
    }

    private String buildPartnerAgreementMessage(DeliveryMode mode) {
        if (mode == DeliveryMode.DELIVERY_PARTNER) {
            return "Both sides agreed to delivery partner support. Choose a delivery partner to continue.";
        }
        return "Both sides agreed to NGO delivery. Choose an NGO partner to continue.";
    }

    private String buildPartnerAssignmentRequiredMessage(DeliveryMode mode) {
        if (mode == DeliveryMode.DELIVERY_PARTNER) {
            return "A delivery partner must be assigned before pickup can start";
        }
        return "An NGO partner must be assigned before pickup can start";
    }
}
