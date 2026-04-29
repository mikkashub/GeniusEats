package com.example.ingredient_management;

import com.example.pantry.PantryData;
import com.example.pantry.PantryInputServiceApi;

/**
 * @author Samariah Hamilton
 * @version 04-17-2026
 * Service implementation for handling pantry input operations.
 * Validates ingredient names and saves them to the repository.
 */
public class InputService implements PantryInputServiceApi {
    private ValidateIngredients validator;
    private PantryData repository;

    public InputService(
            ValidateIngredients validator,
            PantryData repository
    ) {
        this.validator = validator;
        this.repository = repository;
    }

    @Override
    public boolean addIngredient(String ingredientName) {
        if (validator == null) {
            throw new IllegalStateException("Validator is null");
        }
        ValidationResultNames result =
                validator.validate(ingredientName);

        if (result != ValidationResultNames.VALID) {
            return false;
        }

        if (repository == null) {
            throw new IllegalStateException("Repository is null");
        }

        repository.saveIngredient(ingredientName);

        return true;
    }
}
