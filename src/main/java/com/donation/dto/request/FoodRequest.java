package com.donation.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FoodRequest {

    @NotBlank(message = "Donation title is required")
    private String title;

    private String description;

    @NotNull(message = "Donation category is required")
    private String category;

    private String donorDeliveryPreference;

    private String condition;

    private String images;

    @NotNull(message = "Item type is required")
    private String type;

    @Min(value = 1, message = "Quantity must be greater than 0")
    private int quantity;

    @Future(message = "Expiry must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiry;

    @NotBlank(message = "Location is required (format: lat,lng e.g. 28.7,77.1)")
    private String location;
}
