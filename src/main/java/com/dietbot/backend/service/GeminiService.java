package com.dietbot.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.dietbot.backend.model.ChatRequest;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateDietPlan(ChatRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String fullPrompt = String.format(
                "Act as a professional nutritionist and meal planner. Generate a structured diet plan based on the following: " +
                "User Request: %s | Dietary Preferences: %s | Target Calories: %d kcal. " +
                "Format the output using clear headings, calorie breakdowns, and markdown bullet points.",
                request.getPrompt(), request.getDietaryPreferences(), request.getTargetCalories()
        );

        Map<String, Object> part = Map.of("text", fullPrompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> payload = Map.of("contents", List.of(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        String url = GEMINI_BASE_URL + model + ":generateContent?key=" + apiKey;

        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<?, ?> body = response.getBody();
            if (body != null && body.get("candidates") instanceof List<?> candidates
                    && !candidates.isEmpty()
                    && candidates.get(0) instanceof Map<?, ?> firstCandidate
                    && firstCandidate.get("content") instanceof Map<?, ?> contentMap
                    && contentMap.get("parts") instanceof List<?> parts
                    && !parts.isEmpty()
                    && parts.get(0) instanceof Map<?, ?> firstPart
                    && firstPart.get("text") instanceof String text) {
                return text;
            }
        } catch (Exception e) {
            return "Error communicating with AI service: " + e.getMessage();
        }

        return "No response generated.";
    }
}