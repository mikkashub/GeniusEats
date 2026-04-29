package com.example.ingredient_management;

/**
 * Validator interface for validating ingredient names.
 * Provides methods to check if an ingredient name is valid.
 */
public interface IngredientValidatorApi {
    /**
     * Validates an ingredient name.
     * @param ingredientName the name of the ingredient to validate
     * @return the result of the validation
     */
    ValidationResultNames validate(String ingredientName);

}
