package com.geniuseats.backend.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.geniuseats.backend.dto.LoginRequest;
import com.geniuseats.backend.dto.RegisterRequest;
import com.geniuseats.backend.entity.User;
import com.geniuseats.backend.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Map<String, Object> register(RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (request.email == null || request.email.isBlank() ||
            request.password == null || request.password.isBlank()) {
            response.put("success", false);
            response.put("message", "Email and password are required");
            return response;
        }

        if (userRepository.existsByEmail(request.email)) {
            response.put("success", false);
            response.put("message", "Email already exists");
            return response;
        }

        String hashedPassword = passwordEncoder.encode(request.password);
        User user = new User(request.email, hashedPassword);
        userRepository.save(user);

        response.put("success", true);
        response.put("message", "User registered successfully");
        response.put("userId", user.getId());
        response.put("email", user.getEmail());

        return response;
    }

    public Map<String, Object> login(LoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (request.email == null || request.email.isBlank() ||
            request.password == null || request.password.isBlank()) {
            response.put("success", false);
            response.put("message", "Email and password are required");
            return response;
        }

        User user = userRepository.findByEmail(request.email).orElse(null);

        if (user == null) {
            response.put("success", false);
            response.put("message", "User not found");
            return response;
        }

        if (!passwordEncoder.matches(request.password, user.getPassword())) {
            response.put("success", false);
            response.put("message", "Invalid password");
            return response;
        }

        response.put("success", true);
        response.put("message", "Login successful");
        response.put("userId", user.getId());
        response.put("email", user.getEmail());

        return response;
    }
    public Map<String, Object> deleteUser(Long id) {
        Map<String, Object> response = new HashMap<>();
        if (!userRepository.existsById(id)) {
            response.put("success", false);
            response.put("message", "User not found");
            return response;
        }
        userRepository.deleteById(id);
        response.put("success", true);
        response.put("message", "Account deleted.");
        return response;
    }
}