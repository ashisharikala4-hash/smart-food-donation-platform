package com.donation;

import com.donation.enums.DeliveryMode;
import com.donation.enums.DonationCategory;
import com.donation.enums.FoodStatus;
import com.donation.enums.Role;
import com.donation.model.Food;
import com.donation.model.User;
import com.donation.repository.FoodRepository;
import com.donation.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import com.donation.repository.NgoRepository;
import com.donation.model.Ngo;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FoodRepository foodRepository;
    private final NgoRepository ngoRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, FoodRepository foodRepository, NgoRepository ngoRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.foodRepository = foodRepository;
        this.ngoRepository = ngoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedNgos();
        seedUsers();
        seedFood();
    }

    private void seedNgos() {
        if (ngoRepository.count() == 0) {
            ngoRepository.save(Ngo.builder().name("FoodForAll Foundation").contact("contact@foodforall.org").location("28.61,77.20").available(true).build());
            ngoRepository.save(Ngo.builder().name("HungerFree India").contact("help@hungerfree.in").location("28.71,77.10").available(true).build());
            ngoRepository.save(Ngo.builder().name("MealShare NGO").contact("pickup@mealshare.ngo").location("28.65,77.15").available(true).build());
        }
    }

    private void seedUsers() {
        if (userRepository.count() > 0) return;
        userRepository.save(User.builder()
                .name("Platform Admin")
                .email("admin@fooddonate.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .mobileNumber("9999999999")
                .state("Delhi")
                .district("Central Delhi")
                .fullAddress("Platform Office, Connaught Place, New Delhi")
                .pinCode("110001")
                .location("28.6,77.2")
                .verificationStatus("approved")
                .verified(true)
                .build());

        userRepository.save(User.builder()
                .name("Rajesh Kumar")
                .email("donor@fooddonate.com")
                .password(passwordEncoder.encode("donor123"))
                .role(Role.DONOR)
                .mobileNumber("9876543210")
                .state("Delhi")
                .district("North Delhi")
                .fullAddress("221 Civil Lines, Delhi")
                .pinCode("110054")
                .location("28.70,77.10")
                .donationType("BOTH")
                .availability("09:00-13:00, 17:00-20:00")
                .preferredCategories("VEG,SOUPS,NUTRITIOUS")
                .verificationStatus("approved")
                .verified(true)
                .build());

        userRepository.save(User.builder()
                .name("Asha Old Age Home")
                .email("asha@fooddonate.com")
                .password(passwordEncoder.encode("org123"))
                .role(Role.ORGANIZATION)
                .mobileNumber("9811111111")
                .state("Delhi")
                .district("East Delhi")
                .fullAddress("14 Community Care Road, Laxmi Nagar, Delhi")
                .pinCode("110092")
                .location("28.65,77.15")
                .orgName("Asha Old Age Home")
                .contactPersonName("Meera Sharma")
                .orgType("OLD_AGE_HOME")
                .registrationNumber("DL-ASHA-2020-19")
                .capacity(120)
                .foodRequirement(180)
                .acceptedCategories("VEG,NON_VEG,SOUPS,NUTRITIOUS,SPICY,OTHER")
                .storageCapacity("VEG:90,NON_VEG:60,SOUPS:40,NUTRITIOUS:50,SPICY:40,OTHER:30")
                .verificationStatus("approved")
                .documentUrl("seed://asha-verification.pdf")
                .pickupAvailable(true)
                .deliveryRequired(true)
                .verified(true)
                .build());

        userRepository.save(User.builder()
                .name("Hope Orphanage")
                .email("hope@fooddonate.com")
                .password(passwordEncoder.encode("org123"))
                .role(Role.ORGANIZATION)
                .mobileNumber("9822222222")
                .state("Delhi")
                .district("South Delhi")
                .fullAddress("9 Hope Lane, Saket, Delhi")
                .pinCode("110017")
                .location("28.72,77.18")
                .orgName("Hope Orphanage")
                .contactPersonName("Arun Thomas")
                .orgType("ORPHANAGE")
                .registrationNumber("DL-HOPE-2021-08")
                .capacity(75)
                .foodRequirement(120)
                .acceptedCategories("VEG,NON_VEG,SOUPS,NUTRITIOUS")
                .storageCapacity("VEG:70,NON_VEG:40,SOUPS:30,NUTRITIOUS:50")
                .verificationStatus("pending")
                .documentUrl("seed://hope-verification.pdf")
                .pickupAvailable(false)
                .deliveryRequired(true)
                .verified(false)
                .build());
    }

    private void seedFood() {
        if (foodRepository.count() > 0) return;
        
        userRepository.findByEmail("donor@fooddonate.com").ifPresent(donor -> {
            userRepository.findByEmail("asha@fooddonate.com").ifPresent(org1 -> {
                foodRepository.save(Food.builder()
                        .donor(donor)
                        .title("Fresh cooked vegetarian meals")
                        .description("Prepared lunch packets suitable for families and shelters.")
                        .category(DonationCategory.VEG)
                        .itemType("MEALS")
                        .quantity(50)
                        .condition(null)
                        .expiry(LocalDateTime.now().plusHours(4))
                        .location("28.70,77.10")
                        .deliveryMode(null)
                        .status(FoodStatus.POSTED)
                        .createdAt(LocalDateTime.now().minusHours(2))
                        .build());

                foodRepository.save(Food.builder()
                        .donor(donor)
                        .title("Nutritious dal khichdi packs")
                        .description("Comfort food suitable for children and elderly; packed in sealed containers.")
                        .category(DonationCategory.NUTRITIOUS)
                        .itemType("TIFFIN")
                        .quantity(35)
                        .condition(null)
                        .expiry(LocalDateTime.now().plusHours(3))
                        .location("28.70,77.10")
                        .deliveryMode(null)
                        .status(FoodStatus.POSTED)
                        .createdAt(LocalDateTime.now().minusHours(1))
                        .build());

                foodRepository.save(Food.builder()
                        .donor(donor)
                        .title("Spicy snack boxes (packed)")
                        .description("Sealed snack boxes; please confirm dietary restrictions before serving.")
                        .category(DonationCategory.SPICY)
                        .itemType("SNACKS")
                        .quantity(20)
                        .condition(null)
                        .expiry(LocalDateTime.now().plusHours(6))
                        .location("28.70,77.10")
                        .deliveryMode(DeliveryMode.NGO_DELIVERY)
                        .status(FoodStatus.ACCEPTED)
                        .acceptedBy(org1)
                        .createdAt(LocalDateTime.now().minusHours(6))
                        .build());
            });
        });
    }
}
