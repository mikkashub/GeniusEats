package com.example;

import io.javalin.Javalin;
import java.sql.*;
import java.security.MessageDigest;

public class UserAuth {

    private static final String DB_URL = "jdbc:sqlite:survey.db";

    // Creates the users table if it doesn't exist
    public static void initDatabase() {
        String sql =
            "CREATE TABLE IF NOT EXISTS users (" +
            "  id       INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  name     TEXT    NOT NULL," +
            "  email    TEXT    NOT NULL UNIQUE," +
            "  password TEXT    NOT NULL" +
            ");";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println("Error creating users table: " + e.getMessage());
        }
    }

    // Simple SHA-256 hash for passwords
    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed");
        }
    }

    public static void registerRoutes(Javalin app) {

        // POST /signup
        app.post("/signup", ctx -> {
            SignupRequest req = ctx.bodyAsClass(SignupRequest.class);

            if (req.name == null || req.name.trim().isEmpty() ||
                req.email == null || req.email.trim().isEmpty() ||
                req.password == null || req.password.length() < 6) {
                ctx.status(400).result("Name, email and password (min 6 chars) are required.");
                return;
            }

            String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, req.name.trim());
                ps.setString(2, req.email.trim().toLowerCase());
                ps.setString(3, hashPassword(req.password));
                ps.executeUpdate();
                ctx.status(201).result("Account created.");
            } catch (SQLException e) {
                if (e.getMessage().contains("UNIQUE")) {
                    ctx.status(409).result("An account with that email already exists.");
                } else {
                    ctx.status(500).result("Failed to create account.");
                }
            }
        });

        // POST /login
        app.post("/login", ctx -> {
            LoginRequest req = ctx.bodyAsClass(LoginRequest.class);

            if (req.email == null || req.password == null) {
                ctx.status(400).result("Email and password are required.");
                return;
            }

            String sql = "SELECT id, name FROM users WHERE email = ? AND password = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, req.email.trim().toLowerCase());
                ps.setString(2, hashPassword(req.password));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    ctx.status(200).result("{\"id\":" + id + ",\"name\":\"" + name + "\"}");
                } else {
                    ctx.status(401).result("Incorrect email or password.");
                }
            } catch (SQLException e) {
                ctx.status(500).result("Login failed.");
            }
        });

        // DELETE /user/{id}
        app.delete("/user/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            String sql = "DELETE FROM users WHERE id = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                if (rows > 0) ctx.status(200).result("Account deleted.");
                else ctx.status(404).result("User not found.");
            } catch (SQLException e) {
                ctx.status(500).result("Failed to delete account.");
            }
        });
    }

    public static class SignupRequest {
        public String name;
        public String email;
        public String password;
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }
}