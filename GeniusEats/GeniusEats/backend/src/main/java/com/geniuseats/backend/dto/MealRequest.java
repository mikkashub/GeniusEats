package com.geniuseats.backend.dto;

public class MealRequest {
    public Long userId;
    public String mealType;    // "breakfast", "lunch", or "dinner"
    public String extraNote;   // optional special request
    public String pantry;      // comma-separated ingredients e.g. "chicken,rice,garlic"
    public String dietary;     // dietary restrictions e.g. "vegan,gluten-free"
    public String allergies;   // allergies e.g. "peanuts,dairy"
    public String skillLevel;  // "beginner", "intermediate", "advanced"
}
