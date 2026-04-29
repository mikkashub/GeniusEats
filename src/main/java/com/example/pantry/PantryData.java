package com.example.pantry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.ingredient_management.IngredientRepositoryApi;

/**
 * @author Lucas Dunne
 * 
 * This is the data implementation for handling pantry operations. It provides methods to manage ingredients in the pantry.
 */

public class PantryData implements IngredientRepositoryApi {
    private Set<String> pantry = new HashSet<>();

    @Override
    public void saveIngredient(String ingredientName) {
        pantry.add(ingredientName);
    }

    @Override
    public void removeIngredient(String ingredientName) {
        pantry.remove(ingredientName);
    }

    @Override
    public List<String> getAllIngredients() {
        return new ArrayList<>(pantry);
    }

    @Override
    public boolean exists(String ingredientName) {
        return pantry.contains(ingredientName);
    }
}
