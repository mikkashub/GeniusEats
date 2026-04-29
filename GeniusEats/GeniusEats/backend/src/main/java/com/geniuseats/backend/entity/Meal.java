package com.geniuseats.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "meals")
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String mealType; // breakfast, lunch, dinner

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String ingredients; // JSON string

    @Column(columnDefinition = "TEXT")
    private String instructions; // JSON string

    @Column(nullable = false)
    private int prepTime;

    @Column(nullable = false)
    private int cookTime;

    @Column(nullable = false)
    private int servings;

    @Column(nullable = false)
    private int calories;

    @Column(nullable = false)
    private int protein;

    @Column(nullable = false)
    private int carbs;

    @Column(nullable = false)
    private int fat;

    @Column(nullable = false)
    private boolean saved = false;

    @Column(columnDefinition = "TEXT")
    private String tags; // JSON string

    public Meal() {}

    // Getters
    public Long getId()           { return id; }
    public Long getUserId()       { return userId; }
    public String getMealType()   { return mealType; }
    public String getName()       { return name; }
    public String getDescription(){ return description; }
    public String getIngredients(){ return ingredients; }
    public String getInstructions(){ return instructions; }
    public int getPrepTime()      { return prepTime; }
    public int getCookTime()      { return cookTime; }
    public int getServings()      { return servings; }
    public int getCalories()      { return calories; }
    public int getProtein()       { return protein; }
    public int getCarbs()         { return carbs; }
    public int getFat()           { return fat; }
    public boolean isSaved()      { return saved; }
    public String getTags()       { return tags; }

    // Setters
    public void setId(Long id)                  { this.id = id; }
    public void setUserId(Long userId)          { this.userId = userId; }
    public void setMealType(String mealType)    { this.mealType = mealType; }
    public void setName(String name)            { this.name = name; }
    public void setDescription(String d)        { this.description = d; }
    public void setIngredients(String i)        { this.ingredients = i; }
    public void setInstructions(String i)       { this.instructions = i; }
    public void setPrepTime(int prepTime)       { this.prepTime = prepTime; }
    public void setCookTime(int cookTime)       { this.cookTime = cookTime; }
    public void setServings(int servings)       { this.servings = servings; }
    public void setCalories(int calories)       { this.calories = calories; }
    public void setProtein(int protein)         { this.protein = protein; }
    public void setCarbs(int carbs)             { this.carbs = carbs; }
    public void setFat(int fat)                 { this.fat = fat; }
    public void setSaved(boolean saved)         { this.saved = saved; }
    public void setTags(String tags)            { this.tags = tags; }
}
