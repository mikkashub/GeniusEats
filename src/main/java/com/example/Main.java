package com.example;

import com.example.ingredient_management.PantryService;

public class Main {
    public static void main(String[] args) {
        // Step 1: Create database tables
        UserSurvey.initDatabase();
        UserAuth.initDatabase();
        PantryService.initDatabase();

        // Step 2: Start the Javalin REST API server
        UserSurvey.startApi();
    }
}