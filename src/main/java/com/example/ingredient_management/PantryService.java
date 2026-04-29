package com.example.ingredient_management;

import io.javalin.Javalin;
import java.sql.*;
import java.util.*;

public class PantryService {

    private static final String DB_URL = "jdbc:sqlite:survey.db";

    // Creates pantry and allowed_ingredients tables
    public static void initDatabase() {
        String pantryTable =
            "CREATE TABLE IF NOT EXISTS pantry (" +
            "  id         INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  user_id    INTEGER NOT NULL," +
            "  ingredient TEXT    NOT NULL," +
            "  UNIQUE(user_id, ingredient)" +
            ");";

        String allowedTable =
            "CREATE TABLE IF NOT EXISTS allowed_ingredients (" +
            "  id   INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  name TEXT NOT NULL UNIQUE" +
            ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(pantryTable);
            stmt.execute(allowedTable);
            seedAllowedIngredients(conn);
            System.out.println("Pantry tables ready.");
        } catch (SQLException e) {
            System.out.println("Error creating pantry tables: " + e.getMessage());
        }
    }

    // Seeds some default allowed ingredients if the table is empty
    private static void seedAllowedIngredients(Connection conn) throws SQLException {
        String check = "SELECT COUNT(*) FROM allowed_ingredients";
        ResultSet rs = conn.createStatement().executeQuery(check);
        if (rs.next() && rs.getInt(1) > 0) return; // already seeded

        String[] defaults = {
            "chicken", "beef", "pork", "salmon", "tuna", "eggs", "milk", "butter",
            "cheese", "yogurt", "rice", "pasta", "bread", "flour", "oats",
            "tomato", "onion", "garlic", "potato", "carrot", "broccoli", "spinach",
            "lettuce", "cucumber", "pepper", "mushroom", "corn", "peas", "beans",
            "lemon", "lime", "apple", "banana", "strawberry", "blueberry",
            "olive oil", "salt", "black pepper", "cumin", "paprika", "cinnamon",
            "sugar", "honey", "soy sauce", "vinegar", "ketchup", "mustard",
            "peanuts", "almonds", "walnuts", "tofu"
        };

        String insert = "INSERT OR IGNORE INTO allowed_ingredients (name) VALUES (?)";
        PreparedStatement ps = conn.prepareStatement(insert);
        for (String ingredient : defaults) {
            ps.setString(1, ingredient);
            ps.executeUpdate();
        }
    }

    public static void registerRoutes(Javalin app) {

        // GET /pantry/{userId}
        // Returns all ingredients in a user's pantry
        app.get("/pantry/{userId}", ctx -> {
            int userId = Integer.parseInt(ctx.pathParam("userId"));
            List<String> ingredients = getPantryIngredients(userId);
            ctx.status(200).result(toJsonArray(ingredients));
        });

        // POST /pantry/{userId}
        // Adds an ingredient to the user's pantry
        // Body: { "ingredient": "tomato" }
        app.post("/pantry/{userId}", ctx -> {
            int userId = Integer.parseInt(ctx.pathParam("userId"));
            IngredientRequest req = ctx.bodyAsClass(IngredientRequest.class);

            if (req.ingredient == null || req.ingredient.isBlank()) {
                ctx.status(400).result("Ingredient name is required.");
                return;
            }

            String name = req.ingredient.trim().toLowerCase();

            // Load allowed ingredients and current pantry from DB
            Set<String> allowed = getAllowedIngredients();
            Set<String> current = new HashSet<>(getPantryIngredients(userId));

            // Use your ValidateIngredients class
            ValidateIngredients validator =
                new ValidateIngredients(allowed, current);

            ValidationResultNames result = validator.validate(name);

            switch (result) {
                case EMPTY_INPUT:
                    ctx.status(400).result("Ingredient name cannot be empty.");
                    return;
                case UNKNOWN_INGREDIENT:
                    ctx.status(400).result("\"" + name + "\" is not a recognised ingredient.");
                    return;
                case DUPLICATE:
                    ctx.status(409).result("\"" + name + "\" is already in your pantry.");
                    return;
                default:
                    break;
            }

            // Save to DB
            String sql = "INSERT OR IGNORE INTO pantry (user_id, ingredient) VALUES (?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setString(2, name);
                ps.executeUpdate();
                ctx.status(201).result("\"" + name + "\" added to pantry.");
            } catch (SQLException e) {
                ctx.status(500).result("Failed to save ingredient.");
            }
        });

        // DELETE /pantry/{userId}/{ingredient}
        // Removes an ingredient from the user's pantry
        app.delete("/pantry/{userId}/{ingredient}", ctx -> {
            int userId = Integer.parseInt(ctx.pathParam("userId"));
            String ingredient = ctx.pathParam("ingredient").toLowerCase();

            String sql = "DELETE FROM pantry WHERE user_id = ? AND ingredient = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setString(2, ingredient);
                int rows = ps.executeUpdate();
                if (rows > 0) ctx.status(200).result("\"" + ingredient + "\" removed.");
                else ctx.status(404).result("\"" + ingredient + "\" not found in pantry.");
            } catch (SQLException e) {
                ctx.status(500).result("Failed to remove ingredient.");
            }
        });

        // GET /ingredients
        // Returns the full allowed ingredients list (for autocomplete in the UI)
        app.get("/ingredients", ctx -> {
            Set<String> allowed = getAllowedIngredients();
            List<String> sorted = new ArrayList<>(allowed);
            Collections.sort(sorted);
            ctx.status(200).result(toJsonArray(sorted));
        });
    }

    // Load all ingredients in a user's pantry from DB
    private static List<String> getPantryIngredients(int userId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT ingredient FROM pantry WHERE user_id = ? ORDER BY ingredient";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getString("ingredient"));
        } catch (SQLException e) {
            System.out.println("Error loading pantry: " + e.getMessage());
        }
        return list;
    }

    // Load all allowed ingredients from DB
    private static Set<String> getAllowedIngredients() {
        Set<String> set = new HashSet<>();
        String sql = "SELECT name FROM allowed_ingredients";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) set.add(rs.getString("name"));
        } catch (SQLException e) {
            System.out.println("Error loading allowed ingredients: " + e.getMessage());
        }
        return set;
    }

    // Converts a list of strings to a JSON array
    private static String toJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            sb.append("\"").append(items.get(i)).append("\"");
            if (i < items.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static class IngredientRequest {
        public String ingredient;
    }
}