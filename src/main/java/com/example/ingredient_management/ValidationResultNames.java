package com.example.ingredient_management;

/**
 * Enumeration representing the result of ingredient validation.
 */
public enum ValidationResultNames {
    /** The ingredient name is valid. */
    VALID,
    /** The input is empty. */
    EMPTY_INPUT,
    /** The ingredient is a duplicate. */
    DUPLICATE,
    /** The ingredient is unknown. */
    UNKNOWN_INGREDIENT
}
