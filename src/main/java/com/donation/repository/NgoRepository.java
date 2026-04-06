package com.donation.repository;

import com.donation.model.Ngo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NgoRepository extends JpaRepository<Ngo, Long> {
    List<Ngo> findByAvailableTrue();
}
