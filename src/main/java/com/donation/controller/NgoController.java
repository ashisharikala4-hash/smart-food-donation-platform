package com.donation.controller;

import com.donation.dto.response.DeliveryResponse;
import com.donation.model.Ngo;
import com.donation.repository.NgoRepository;
import com.donation.service.DeliveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ngo")
public class NgoController {

    private final NgoRepository ngoRepository;
    private final DeliveryService deliveryService;

    public NgoController(NgoRepository ngoRepository, DeliveryService deliveryService) {
        this.ngoRepository = ngoRepository;
        this.deliveryService = deliveryService;
    }

    @GetMapping("/available")
    public ResponseEntity<List<Ngo>> getAvailableNgos() {
        return ResponseEntity.ok(ngoRepository.findByAvailableTrue());
    }

    @PostMapping("/assign/{foodId}")
    public ResponseEntity<DeliveryResponse> assignNgo(
            @PathVariable Long foodId,
            @RequestParam Long ngoId) {
        return ResponseEntity.ok(deliveryService.assignNgo(foodId, ngoId));
    }
}
