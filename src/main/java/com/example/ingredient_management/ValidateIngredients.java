package com.example.ingredient_management;

import java.util.Set;

/**
 * @author Samariah Hamilton
 * @version 04-17-2026
 * Implementation of ingredient validation logic.
 * Checks if ingredient names are valid, not empty, and not already in the pantry.
 */
public class ValidateIngredients implements IngredientValidatorApi {
    private Set<String> allowedIngredients;
    private Set<String> pantryIngredients;

    public ValidateIngredients(
            Set<String> allowedIngredients,
            Set<String> pantryIngredients
    ) {
        this.allowedIngredients = allowedIngredients;
        this.pantryIngredients = pantryIngredients;
    }

    @Override
    public ValidationResultNames validate(String ingredientName) {

        if (ingredientName == null || ingredientName.isBlank()) {
            return ValidationResultNames.EMPTY_INPUT;
        }

        if (!allowedIngredients.contains(ingredientName)) {
            return ValidationResultNames.UNKNOWN_INGREDIENT;
        }

        if (pantryIngredients.contains(ingredientName)) {
            return ValidationResultNames.DUPLICATE;
        }

        return ValidationResultNames.VALID;
    }

    
}
