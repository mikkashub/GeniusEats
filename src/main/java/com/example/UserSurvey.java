package com.example;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.example.ingredient_management.PantryService;

/**
 * @author Malkiel Parr
 * 
 * This class represents a user's dietary survey, including their diet type, allergens, and budget. It provides methods to save, update, load, and delete survey data in a SQLite database. Additionally, it sets up a REST API using Javalin to allow the front end to interact with the survey data through HTTP requests.
 */

/**
 * UserSurvey - Handles collecting and storing the user's dietary preferences.
 *
 * Three main things:
 *   1. Defines the data model (diet, allergens, budget)
 *   2. Sets up a SQLite database to save that data permanently
 *   3. Exposes a REST API via Javalin so the front end can call it
 */
public class UserSurvey {

    // Section 1: Data fields
    // These represent one user's survey answers.

    private int          id;        // Auto-assigned by the database
    private String       diet;      // e.g. "vegan", "none"
    private List<String> allergens; // e.g. ["peanuts", "gluten"]
    private int          budget;    // 1 = small, 2 = medium, 3 = large

    // Section 2: Database setup
    // SQLite stores everything in a single local file called "survey.db"
    // No separate database server is needed.

    private static final String DB_URL = "jdbc:sqlite:survey.db";

    /**
     * Runs once at startup
     * Creates the 'user_surveys' table if it doesn't already exist
     *
     * Columns:
     *   id        - auto-incremented primary key (database assigns this)
     *   diet      - text value for the chosen diet
     *   allergens - comma-separated string, e.g. "peanuts,gluten"
     *   budget    - integer: 1, 2, or 3
     */

    public static void initDatabase() {
        String createTableSQL =
                "CREATE TABLE IF NOT EXISTS user_surveys (" +
                        "  id        INTEGER PRIMARY KEY ," +
                        "  diet      TEXT    NOT NULL," +
                        "  allergens TEXT    NOT NULL," +
                        "  budget    INTEGER NOT NULL" +
                        ");";

        // try-with-resources automatically closes the connection when done
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSQL);
            System.out.println("Database ready.");

        } catch (SQLException e) {
            System.out.println("Error setting up database: " + e.getMessage());
        }
    }

    // Section 3: Database operations (save / update / load / delete)

    /**
     * Saves current survey into the database
     * Uses PreparedStatement placeholders (?) to prevent SQL injection
     * Returns the new row's auto generated ID or -1 ,if something went wrong during the run
     */
    public int saveToDatabase() {
        String insertSQL =
                "INSERT INTO user_surveys (diet, allergens, budget) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     insertSQL, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, this.diet);
            pstmt.setString(2, String.join(",", this.allergens)); // List -> "a,b,c"
            pstmt.setInt(3, this.budget);
            pstmt.executeUpdate();

            // Get the ID the database assigned to this new row
            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                this.id = keys.getInt(1);
                System.out.println("Survey saved with ID: " + this.id);
                return this.id;
            }

        } catch (SQLException e) {
            System.out.println("Error saving survey: " + e.getMessage());
        }

        return -1; // signals failure
    }

    /**
     * Update an existing survey row in the database
     * Is used when the user wants to change their saved prefs
     * Returns true if update succeeded
     */
    public boolean updateInDatabase() {
        String updateSQL =
                "UPDATE user_surveys SET diet = ?, allergens = ?, budget = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {

            pstmt.setString(1, this.diet);
            pstmt.setString(2, String.join(",", this.allergens));
            pstmt.setInt(3, this.budget);
            pstmt.setInt(4, this.id);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0; // true if at least one row changed

        } catch (SQLException e) {
            System.out.println("Error updating survey: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load a survey from the database by its ID
     * Return a populated UserSurvey, or null if ID isn't found
     */

    public static UserSurvey loadFromDatabase(int surveyId) {
        String selectSQL = "SELECT * FROM user_surveys WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setInt(1, surveyId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                UserSurvey survey  = new UserSurvey();
                survey.id          = rs.getInt("id");
                survey.diet        = rs.getString("diet");
                survey.budget      = rs.getInt("budget");
                survey.allergens   = parseAllergens(rs.getString("allergens"));
                return survey;
            }

        } catch (SQLException e) {
            System.out.println("Error loading survey: " + e.getMessage());
        }

        return null; // nothing found for that ID
    }

    /**
     * Delete a survey row from the database by its ID
     * Return true if a row was removed properly
     */

    public static boolean deleteFromDatabase(int surveyId) {
        String deleteSQL = "DELETE FROM user_surveys WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {

            pstmt.setInt(1, surveyId);
            int rows = pstmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.out.println("Error deleting survey: " + e.getMessage());
            return false;
        }
    }

    // Section 4: Javalin REST API
    // Javalin listens on port 7070 and handles HTTP requests from the front end.
    //
    // Routes:
    //   POST   /survey        --> create a new survey
    //   GET    /survey/{id}   --> fetch a survey by ID
    //   PUT    /survey/{id}   --> update an existing survey
    //   DELETE /survey/{id}   --> delete a survey

    /**
     * Start Javalin web server on port 7070
     * It's called from main() to bring API online
     */
    public static void startApi() {

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> rule.anyHost());
            });
        }).start(7070);
        UserAuth.registerRoutes(app);
        PantryService.registerRoutes(app);

        // POST /survey
        // Creates a new survey from a JSON body.
        //
        // Expected JSON:
        // {
        //   "diet": "vegan",
        //   "allergens": ["peanuts", "gluten"],
        //   "budget": 2
        // }

        app.post("/survey", ctx -> {
            UserSurvey survey = parseSurveyFromRequest(ctx);

            // Reject the request if budget is not 1, 2, or 3
            if (!isValidBudget(survey.budget)) {
                ctx.status(400).result("Budget must be 1, 2, or 3.");
                return;
            }

            int newId = survey.saveToDatabase();

            if (newId != -1) {
                ctx.status(201).result("Survey saved! Your survey ID is: " + newId);
            } else {
                ctx.status(500).result("Failed to save survey.");
            }
        });

        // GET /survey/{id}
        // Returns the survey matching the given ID as JSON.
        // Ex: GET /survey/3

        app.get("/survey/{id}", ctx -> {
            // Pull the {id} value out of the URL
            int id = Integer.parseInt(ctx.pathParam("id"));

            UserSurvey survey = loadFromDatabase(id);

            if (survey != null) {
                ctx.status(200).result(survey.toJson());
            } else {
                ctx.status(404).result("Survey with ID " + id + " not found.");
            }
        });

        // PUT /survey/{id}
        // Replaces the survey data for an existing ID.
        // Ex: PUT /survey/3  (body: same JSON format as POST)

        app.put("/survey/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));

            // Check the survey exists before trying to update it
            UserSurvey existing = loadFromDatabase(id);
            if (existing == null) {
                ctx.status(404).result("Survey with ID " + id + " not found.");
                return;
            }

            // Build a new survey from the request body
            UserSurvey updated = parseSurveyFromRequest(ctx);
            updated.id = id; // make sure we update the right row

            if (!isValidBudget(updated.budget)) {
                ctx.status(400).result("Budget must be 1, 2, or 3.");
                return;
            }

            boolean success = updated.updateInDatabase();
            if (success) {
                ctx.status(200).result("Survey updated successfully.");
            } else {
                ctx.status(500).result("Failed to update survey.");
            }
        });

        // DELETE /survey/{id}
        // Permanently removes a survey from the database.
        // Example: DELETE /survey/3

        app.delete("/survey/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));

            boolean deleted = deleteFromDatabase(id);

            if (deleted) {
                ctx.status(200).result("Survey deleted.");
            } else {
                ctx.status(404).result("Survey with ID " + id + " not found.");
            }
        });

        System.out.println("API running at http://localhost:7070");
    }

    // Section 5: Helper methods

    /**
     * Read the JSON body from the HTTP request and returns a UserSurvey object
     * Javalin's bodyAsClass() uses Jackson to automatically map each JSON key
     * to the matching field in SurveyRequest (names must match, EXACTLY)
     */
    private static UserSurvey parseSurveyFromRequest(Context ctx) {
        // Javalin parses the JSON body into our simple SurveyRequest class
        SurveyRequest req = ctx.bodyAsClass(SurveyRequest.class);

        // Copy the values from the request into a proper UserSurvey object
        UserSurvey survey = new UserSurvey();

        if (req.diet != null) {
            survey.diet = req.diet;
        } else {
            survey.diet = "none"; // default if the field was missing
        }

        if (req.allergens != null) {
            survey.allergens = req.allergens;
        } else {
            survey.allergens = new ArrayList<>(); // default to empty list
        }

        survey.budget = req.budget;

        return survey;
    }

    /**
     * Simple data class used to deserialize incoming JSON request bodies
     * Field names must match the JSON keys exactly so Jackson can map them
     */
    public static class SurveyRequest {
        public String       diet;
        public List<String> allergens;
        public int          budget;
    }

    /**
     * Check that the budget value is 1, 2, or 3
     * Anything outside the range gets rejected with an HTTP 400 error
     */
    private static boolean isValidBudget(int budget) {
        if (budget < 1 || budget > 3) {
            return false;
        }
        return true;
    }

    /**
     * Convert the comma separated allergen string stored in SQLite
     * back into a Java List so the rest of the code can work with it normally
     *
     * Ex: "peanuts,gluten" -> ["peanuts", "gluten"]
     * If the string is empty or "none", just returns an empty list
     */
    private static List<String> parseAllergens(String raw) {
        List<String> list = new ArrayList<>();

        // Return empty list if there's nothing to parse
        if (raw == null || raw.trim().isEmpty() || raw.equalsIgnoreCase("none")) {
            return list;
        }

        // Split on commas and add each item to the list
        String[] parts = raw.split(",");
        for (String part : parts) {
            list.add(part.trim());
        }

        return list;
    }

    /**
     * Build a JSON string from this survey's fields for API responses
     *
     * Ex output:
     * {"id":1,"diet":"vegan","allergens":["peanuts"],"budget":2}
     */
    public String toJson() {
        String allergensJson = "[";
        for (int i = 0; i < allergens.size(); i++) {
            allergensJson += "\"" + allergens.get(i) + "\"";
            // Add a comma after every item except the last one
            if (i < allergens.size() - 1) {
                allergensJson += ",";
            }
        }
        allergensJson += "]";

        String json = "{" +
                "\"id\":"        + id           + "," +
                "\"diet\":\""    + diet         + "\"," +
                "\"allergens\":" + allergensJson + "," +
                "\"budget\":"    + budget        +
                "}";

        return json;
    }

    // Section 6: Getters & Setters

    public int    getId()                            { return id; }

    public String getDiet()                          { return diet; }
    public void   setDiet(String diet)               { this.diet = diet; }

    public List<String> getAllergens()               { return allergens; }
    public void         setAllergens(List<String> a) { this.allergens = a; }

    /**
     * Budget values:
     *   1 = small  ($10–$30)
     *   2 = medium ($30–$60)
     *   3 = large  ($60–$100)
     */
    public int  getBudget()                          { return budget; }
    public void setBudget(int budget)                { this.budget = budget; }

    // Section 7: Constructor: safe defaults

    public UserSurvey() {
        this.diet      = "none";
        this.allergens = new ArrayList<>();
        this.budget    = 1;
    }
}