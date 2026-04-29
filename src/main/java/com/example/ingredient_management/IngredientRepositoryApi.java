package com.example.ingredient_management;

import java.util.List;

/**
 * Repository interface for managing ingredients in the pantry.
 * Provides methods to save, remove, retrieve, and check existence of ingredients.
 */
public interface IngredientRepositoryApi {
    /**
     * Saves an ingredient to the repository.
     * @param ingredientName the name of the ingredient to save
     */
    void saveIngredient(String ingredientName);

    /**
     * Removes an ingredient from the repository.
     * @param ingredientName the name of the ingredient to remove
     */
    void removeIngredient(String ingredientName);

    /**
     * Retrieves all ingredients from the repository.
     * @return a list of all ingredient names
     */
    List<String> getAllIngredients();

    /**
     * Checks if an ingredient exists in the repository.
     * @param ingredientName the name of the ingredient to check
     * @return true if the ingredient exists, false otherwise
     */
    boolean exists(String ingredientName);

}
