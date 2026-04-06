package com.donation.controller;

import com.donation.dto.request.DeliveryUpdateRequest;
import com.donation.dto.response.DeliveryResponse;
import com.donation.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping("/create/{foodId}")
    public ResponseEntity<DeliveryResponse> createDelivery(@PathVariable Long foodId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deliveryService.createDelivery(foodId));
    }

    @PatchMapping("/update/{deliveryId}")
    public ResponseEntity<DeliveryResponse> updateStatus(
            @PathVariable Long deliveryId,
            @Valid @RequestBody DeliveryUpdateRequest request) {
        return ResponseEntity.ok(deliveryService.updateDeliveryStatus(deliveryId, request));
    }

    @GetMapping("/{foodId}")
    public ResponseEntity<DeliveryResponse> getDelivery(@PathVariable Long foodId) {
        return ResponseEntity.ok(deliveryService.getDeliveryByFoodId(foodId));
    }
}
