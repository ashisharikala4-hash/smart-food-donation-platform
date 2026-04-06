package com.donation.repository;

import com.donation.model.Message;
import com.donation.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByFoodOrderByTimestampAsc(Food food);
    void deleteByFood(Food food);
}
