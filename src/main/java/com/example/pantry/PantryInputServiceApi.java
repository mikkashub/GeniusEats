package com.example.pantry;
/**
 * @author Lucas Dunne
 * 
 * This is the service interface for handling pantry input operations. It provides methods to add ingredients to the pantry.
 */
/**
 * Service interface for handling pantry input operations.
 * Provides methods to add ingredients to the pantry.
 */
public interface PantryInputServiceApi {
    /**
     * Adds an ingredient to the pantry.
     * @param ingredientName the name of the ingredient to add
     * @return true if the ingredient was added successfully, false otherwise
     */
    boolean addIngredient(String ingredientName);

}
