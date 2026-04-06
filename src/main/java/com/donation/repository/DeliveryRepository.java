package com.donation.repository;

import com.donation.model.Delivery;
import com.donation.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByFood(Food food);
    void deleteByFood(Food food);
}
