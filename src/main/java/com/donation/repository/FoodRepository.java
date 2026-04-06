package com.donation.repository;

import com.donation.enums.FoodStatus;
import com.donation.model.Food;
import com.donation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findByStatus(FoodStatus status);
    List<Food> findByDonor(User donor);
    List<Food> findByDonorOrderByExpiryAsc(User donor);
}
