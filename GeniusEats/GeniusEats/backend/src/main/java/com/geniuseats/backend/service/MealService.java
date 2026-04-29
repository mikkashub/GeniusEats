package com.geniuseats.backend.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.geniuseats.backend.dto.MealRequest;
import com.geniuseats.backend.entity.Meal;
import com.geniuseats.backend.repository.MealRepository;

@Service
public class MealService {

    private final MealRepository mealRepository;

    // Set groq.api.key in your application.properties
    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL    = "llama-3.3-70b-versatile";

    public MealService(MealRepository mealRepository) {
        this.mealRepository = mealRepository;
    }

    // ── Generate a meal via Groq AI ───────────────────────────────────────────
    public Map<String, Object> generateMeal(MealRequest request) {
        Map<String, Object> response = new HashMap<>();

        String validationError = validateRequest(request);
        if (validationError != null) {
            response.put("success", false);
            response.put("message", validationError);
            return response;
        }

        String prompt = buildPrompt(request);

        String aiResponse;
        try {
            aiResponse = callGroq(prompt);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to reach AI service: " + e.getMessage());
            return response;
        }

        Meal meal;
        try {
            meal = parseMealFromJson(aiResponse, request);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to parse AI response: " + e.getMessage());
            return response;
        }

        String mealValidationError = validateMeal(meal);
        if (mealValidationError != null) {
            response.put("success", false);
            response.put("message", "Meal validation failed: " + mealValidationError);
            return response;
        }

        mealRepository.save(meal);

        response.put("success", true);
        response.put("message", "Meal generated successfully");
        response.put("meal", mealToMap(meal));
        return response;
    }

    // ── Save / unsave a meal ──────────────────────────────────────────────────
    public Map<String, Object> saveMeal(Long mealId, Long userId) {
        Map<String, Object> response = new HashMap<>();

        Meal meal = mealRepository.findById(mealId).orElse(null);

        if (meal == null) {
            response.put("success", false);
            response.put("message", "Meal not found");
            return response;
        }

        if (!meal.getUserId().equals(userId)) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return response;
        }

        meal.setSaved(!meal.isSaved());
        mealRepository.save(meal);

        response.put("success", true);
        response.put("saved", meal.isSaved());
        response.put("message", meal.isSaved() ? "Meal saved" : "Meal unsaved");
        return response;
    }

    // ── Get all saved meals for a user ────────────────────────────────────────
    public Map<String, Object> getSavedMeals(Long userId) {
        Map<String, Object> response = new HashMap<>();
        var meals = mealRepository.findByUserIdAndSavedTrue(userId);
        response.put("success", true);
        response.put("meals", meals.stream().map(this::mealToMap).toList());
        return response;
    }

    // ── Get the most recently generated meal ──────────────────────────────────
    public Map<String, Object> getCurrentMeal(Long userId) {
        Map<String, Object> response = new HashMap<>();
        Meal meal = mealRepository.findTopByUserIdOrderByIdDesc(userId).orElse(null);

        if (meal == null) {
            response.put("success", false);
            response.put("message", "No meal found");
            return response;
        }

        response.put("success", true);
        response.put("meal", mealToMap(meal));
        return response;
    }
    public Map<String, Object> editAndRegenerate(Long mealId, Long userId, MealRequest request) {
        Map<String, Object> response = new HashMap<>();

        // Step 1: Find the existing meal
        Meal existing = mealRepository.findById(mealId).orElse(null);
        if (existing == null) {
            response.put("success", false);
            response.put("message", "Meal not found");
            return response;
        }

        if (!existing.getUserId().equals(userId)) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return response;
        }

        // Step 2: Use the editMeal logic — validate and update ingredients
        // request.pantry contains the updated ingredients from the frontend
        if (request.pantry == null || request.pantry.isBlank()) {
            response.put("success", false);
            response.put("message", "No ingredients provided");
            return response;
        }

        // Step 3: Keep the same meal type and user prefs, just update pantry
        request.userId   = userId;
        request.mealType = existing.getMealType();

        // Step 4: Regenerate via Groq with updated ingredients
        return generateMeal(request);
    }

    // ── Validate incoming request ─────────────────────────────────────────────
    private String validateRequest(MealRequest request) {
        if (request.userId == null) {
            return "User ID is required";
        }
        if (request.mealType == null || request.mealType.isBlank()) {
            return "Meal type is required";
        }
        String type = request.mealType.toLowerCase();
        if (!type.equals("breakfast") && !type.equals("lunch") && !type.equals("dinner")) {
            return "Meal type must be breakfast, lunch, or dinner";
        }
        return null;
    }

    // ── Validate the parsed meal object ───────────────────────────────────────
    private String validateMeal(Meal meal) {
        if (meal.getName() == null || meal.getName().isBlank()) {
            return "Meal name is missing";
        }
        if (meal.getIngredients() == null || meal.getIngredients().equals("[]")) {
            return "Ingredients are missing";
        }
        if (meal.getInstructions() == null || meal.getInstructions().equals("[]")) {
            return "Instructions are missing";
        }
        if (meal.getCalories() <= 0) {
            return "Calorie count is invalid";
        }
        if (meal.getPrepTime() < 0 || meal.getCookTime() < 0) {
            return "Prep/cook time is invalid";
        }
        if (meal.getServings() <= 0) {
            return "Servings count is invalid";
        }
        return null;
    }

    // ── Build the prompt ──────────────────────────────────────────────────────
    private String buildPrompt(MealRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a creative and delicious ")
              .append(request.mealType)
              .append(" recipe.\n");

        if (request.dietary != null && !request.dietary.isBlank()) {
            prompt.append("Dietary restrictions: ").append(request.dietary).append(".\n");
        }
        if (request.allergies != null && !request.allergies.isBlank()) {
            prompt.append("Allergies to avoid: ").append(request.allergies).append(".\n");
        }
        if (request.pantry != null && !request.pantry.isBlank()) {
            prompt.append("Prioritize these available ingredients: ").append(request.pantry).append(".\n");
        }
        if (request.skillLevel != null && !request.skillLevel.isBlank()) {
            prompt.append("Cooking skill level: ").append(request.skillLevel).append(".\n");
        }
        if (request.extraNote != null && !request.extraNote.isBlank()) {
            prompt.append("Special request: ").append(request.extraNote).append(".\n");
        }

        prompt.append("Make it practical, well-balanced, and include accurate nutrition values per serving.");
        return prompt.toString();
    }

    // ── Call Groq API ─────────────────────────────────────────────────────────
    private String callGroq(String prompt) throws Exception {
        String systemPrompt = "You are a professional chef and nutritionist. " +
            "Always respond with ONLY valid JSON matching this exact structure, no markdown, no extra text:\n" +
            "{\n" +
            "  \"name\": \"string\",\n" +
            "  \"description\": \"string\",\n" +
            "  \"prepTime\": number,\n" +
            "  \"cookTime\": number,\n" +
            "  \"servings\": number,\n" +
            "  \"ingredients\": [{\"name\": \"string\", \"amount\": \"string\", \"unit\": \"string\"}],\n" +
            "  \"instructions\": [\"string\"],\n" +
            "  \"nutrition\": {\"calories\": number, \"protein\": number, \"carbs\": number, \"fat\": number},\n" +
            "  \"tags\": [\"string\"]\n" +
            "}";

        String body = "{"
            + "\"model\":\"" + MODEL + "\","
            + "\"max_tokens\":1500,"
            + "\"messages\":["
            + "{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"},"
            + "{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}"
            + "]}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(GROQ_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + groqApiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() != 200) {
            throw new RuntimeException("Groq API returned status " + httpResponse.statusCode()
                + ": " + httpResponse.body());
        }

        // Extract content from Groq response (same format as OpenAI)
        String responseBody = httpResponse.body();
        String marker = "\"content\":\"";
        int start = responseBody.indexOf(marker);
        if (start == -1) throw new RuntimeException("Could not find content in Groq response");
        start += marker.length();
        int end = responseBody.indexOf("\",\"refusal\"", start);
        if (end == -1) end = responseBody.lastIndexOf("\"");
        String extracted = responseBody.substring(start, end)
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
        System.out.println("=== GROQ RESPONSE ===");
        System.out.println(extracted);
        System.out.println("=== END ===");
        return extracted;
    }

    // ── Parse JSON response into a Meal entity ────────────────────────────────
    private Meal parseMealFromJson(String json, MealRequest request) {
        Meal meal = new Meal();
        meal.setUserId(request.userId);
        meal.setMealType(request.mealType.toLowerCase());
        meal.setSaved(false);

        meal.setName(extractJsonString(json, "name"));
        meal.setDescription(extractJsonString(json, "description"));
        meal.setPrepTime(extractJsonInt(json, "prepTime"));
        meal.setCookTime(extractJsonInt(json, "cookTime"));
        meal.setServings(extractJsonInt(json, "servings"));

        meal.setIngredients(extractJsonArray(json, "ingredients"));
        meal.setInstructions(extractJsonArray(json, "instructions"));
        meal.setTags(extractJsonArray(json, "tags"));

        String nutrition = extractJsonObject(json, "nutrition");
        meal.setCalories(extractJsonInt(nutrition, "calories"));
        meal.setProtein(extractJsonInt(nutrition, "protein"));
        meal.setCarbs(extractJsonInt(nutrition, "carbs"));
        meal.setFat(extractJsonInt(nutrition, "fat"));

        return meal;
    }

    // ── Convert Meal entity to Map for API response ───────────────────────────
    private Map<String, Object> mealToMap(Meal meal) {
        Map<String, Object> map = new HashMap<>();
        map.put("id",           meal.getId());
        map.put("userId",       meal.getUserId());
        map.put("mealType",     meal.getMealType());
        map.put("name",         meal.getName());
        map.put("description",  meal.getDescription());
        map.put("ingredients",  meal.getIngredients());
        map.put("instructions", meal.getInstructions());
        map.put("prepTime",     meal.getPrepTime());
        map.put("cookTime",     meal.getCookTime());
        map.put("servings",     meal.getServings());
        map.put("calories",     meal.getCalories());
        map.put("protein",      meal.getProtein());
        map.put("carbs",        meal.getCarbs());
        map.put("fat",          meal.getFat());
        map.put("saved",        meal.isSaved());
        map.put("tags",         meal.getTags());
        return map;
    }

    // ── JSON parsing helpers ──────────────────────────────────────────────────

    private String extractJsonString(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start == -1) return "";
        start += marker.length();
        // Skip whitespace
        while (start < json.length() && json.charAt(start) != '"') start++;
        if (start >= json.length()) return "";
        start++; // skip opening quote
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }

    private int extractJsonInt(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start == -1) return 0;
        start += marker.length();
        // Skip whitespace
        while (start < json.length() && !Character.isDigit(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (start == end) return 0;
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String extractJsonArray(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start == -1) return "[]";
        start += marker.length();
        // Skip whitespace until we hit the opening bracket
        while (start < json.length() && json.charAt(start) != '[') start++;
        if (start >= json.length()) return "[]";
        int depth = 0;
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) { end++; break; } }
            end++;
        }
        return json.substring(start, end);
    }

    private String extractJsonObject(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start == -1) return "{}";
        start += marker.length();
        // Skip whitespace until we hit the opening brace
        while (start < json.length() && json.charAt(start) != '{') start++;
        if (start >= json.length()) return "{}";
        int depth = 0;
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) { end++; break; } }
            end++;
        }
        return json.substring(start, end);
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}