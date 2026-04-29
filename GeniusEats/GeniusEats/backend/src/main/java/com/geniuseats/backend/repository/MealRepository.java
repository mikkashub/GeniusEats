package com.geniuseats.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.geniuseats.backend.entity.Meal;

public interface MealRepository extends JpaRepository<Meal, Long> {

    // Get all saved meals for a user
    List<Meal> findByUserIdAndSavedTrue(Long userId);

    // Get the most recently generated meal for a user
    Optional<Meal> findTopByUserIdOrderByIdDesc(Long userId);

    // Get all meals for a user
    List<Meal> findByUserId(Long userId);
}
