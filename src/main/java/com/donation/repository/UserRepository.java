package com.donation.repository;

import com.donation.enums.Role;
import com.donation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByRoleAndVerified(Role role, boolean verified);
    boolean existsByEmail(String email);
}
