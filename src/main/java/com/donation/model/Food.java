package com.donation.model;

import com.donation.enums.DeliveryMode;
import com.donation.enums.DonationCategory;
import com.donation.enums.DonationCondition;
import com.donation.enums.FoodStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "food")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "donor_id", nullable = false)
    private User donor;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DonationCategory category;

    private String itemType;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    private DonationCondition condition;

    private LocalDateTime expiry;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String images;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private DeliveryMode deliveryMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private DeliveryMode donorDeliveryPreference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private DeliveryMode organizationDeliveryPreference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "accepted_by")
    private User acceptedBy;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "food_declined_organizations",
            joinColumns = @JoinColumn(name = "food_id"),
            inverseJoinColumns = @JoinColumn(name = "organization_id")
    )
    private Set<User> declinedByOrganizations = new HashSet<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
