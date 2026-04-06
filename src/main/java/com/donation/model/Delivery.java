package com.donation.model;

import com.donation.enums.DeliveryMode;
import com.donation.enums.FoodStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryMode deliveryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodStatus status;

    private double estimatedCost;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_ngo_id")
    private Ngo assignedNgo;
}
