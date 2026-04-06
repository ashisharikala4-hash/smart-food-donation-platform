package com.donation.controller;

import com.donation.dto.request.DeliveryModeRequest;
import com.donation.dto.request.FoodRequest;
import com.donation.dto.response.FoodResponse;
import com.donation.dto.response.MatchResponse;
import com.donation.service.FoodService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/food")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @PostMapping("")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<FoodResponse> addFood(@Valid @RequestBody FoodRequest request, Principal principal) {
        System.out.println("Incoming request: " + request);
        return ResponseEntity.status(HttpStatus.CREATED).body(foodService.addFood(request, principal.getName()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<FoodResponse>> getAllFood() {
        return ResponseEntity.ok(foodService.getAllFood());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<List<FoodResponse>> getMyFood(Principal principal) {
        return ResponseEntity.ok(foodService.getDonorFood(principal.getName()));
    }

    @PostMapping("/accept/{foodId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<FoodResponse> acceptFood(@PathVariable Long foodId, Principal principal) {
        return ResponseEntity.ok(foodService.acceptFood(foodId, principal.getName()));
    }

    @PostMapping("/reject/{foodId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<FoodResponse> rejectFood(@PathVariable Long foodId, Principal principal) {
        return ResponseEntity.ok(foodService.rejectFood(foodId, principal.getName()));
    }

    @PutMapping("/status/{foodId}")
    public ResponseEntity<FoodResponse> updateStatus(
            @PathVariable Long foodId,
            @RequestParam String status,
            Principal principal) {
        return ResponseEntity.ok(foodService.updateStatus(foodId, status, principal.getName()));
    }

    @DeleteMapping("/{foodId}")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<Void> deleteFood(@PathVariable Long foodId, Principal principal) {
        foodService.deleteFood(foodId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/match/{foodId}")
    public ResponseEntity<MatchResponse> getBestMatch(@PathVariable Long foodId) {
        return ResponseEntity.ok(foodService.getBestMatch(foodId));
    }

    @GetMapping("/ranked")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<List<FoodResponse>> getRankedFood(Principal principal) {
        return ResponseEntity.ok(foodService.getRankedFood(principal.getName()));
    }

    @PostMapping("/select-delivery")
    public ResponseEntity<FoodResponse> selectDeliveryMode(
            @Valid @RequestBody DeliveryModeRequest request,
            Principal principal) {
        return ResponseEntity.ok(foodService.selectDeliveryMode(request.getFoodId(), request.getMode(), principal.getName()));
    }
}
