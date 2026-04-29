package com.geniuseats.backend.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geniuseats.backend.dto.MealRequest;
import com.geniuseats.backend.service.MealService;

/**
 * @author Mer Kulang
 * 
 * This controller handles meal-related operations, including generating meals via Claude AI, saving/unsaving meals, editing meals, and retrieving saved/current meals for a user. It uses the MealService to perform the necessary operations and returns the results as a map of string keys and object values.
 */
@RestController
@RequestMapping("/api/meals")
@CrossOrigin(origins = "*")
public class MealController {

    private final MealService mealService;

    public MealController(MealService mealService) {
        this.mealService = mealService;
    }

    // POST /api/meals/generate
    // Generates a new meal via Claude AI and saves it to the database
    // Body: { "userId": 1, "mealType": "dinner", "pantry": "chicken,rice", "dietary": "none", "extraNote": "spicy" }
    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody MealRequest request) {
        return mealService.generateMeal(request);
    }

    // PUT /api/meals/{mealId}/save/{userId}
    // Toggles saved status on a meal (save or unsave)
    @PutMapping("/{mealId}/save/{userId}")
    public Map<String, Object> save(@PathVariable Long mealId, @PathVariable Long userId) {
        return mealService.saveMeal(mealId, userId);
    }

    // PUT /api/meals/{mealId}/edit/{userId}
    // Regenerates a meal with updated ingredients
    @PutMapping("/{mealId}/edit/{userId}")
    public Map<String, Object> edit(@PathVariable Long mealId, @PathVariable Long userId, @RequestBody MealRequest request) {
        return mealService.editAndRegenerate(mealId, userId, request);
    }

    // GET /api/meals/saved/{userId}
    // Returns all saved meals for a user
    @GetMapping("/saved/{userId}")
    public Map<String, Object> getSaved(@PathVariable Long userId) {
        return mealService.getSavedMeals(userId);
    }

    // GET /api/meals/current/{userId}
    // Returns the most recently generated meal for a user
    @GetMapping("/current/{userId}")
    public Map<String, Object> getCurrent(@PathVariable Long userId) {
        return mealService.getCurrentMeal(userId);
    }
}
