package com.dietbot.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.dietbot.backend.model.ChatRequest;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    @Value("${gemini.api.key}")
    private String apiKey;

    // Comma-separated, tried in order until one answers
    @Value("${gemini.models}")
    private String models;

    private final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        return new RestTemplate(factory);
    }

    public String generateDietPlan(ChatRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Key goes in a header, not the URL, so it never shows up in logs or error messages
        headers.set("x-goog-api-key", apiKey);

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

        String lastError = "No response generated.";

        for (String rawModel : models.split(",")) {
            String model = rawModel.trim();
            if (model.isEmpty()) {
                continue;
            }

            try {
                log.info("Calling Gemini model: {}", model);
                long start = System.currentTimeMillis();

                @SuppressWarnings("rawtypes")
                ResponseEntity<Map> response = restTemplate.exchange(
                        GEMINI_BASE_URL + model + ":generateContent",
                        HttpMethod.POST,
                        entity,
                        Map.class
                );

                log.info("{} replied in {} ms", model, System.currentTimeMillis() - start);

                String text = extractText(response.getBody());
                if (text != null) {
                    return text;
                }
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                if (status == 429 || status >= 500) {
                    // Overloaded or rate-limited: try the next model
                    log.warn("Model {} unavailable ({}), trying next model", model, status);
                    lastError = "The AI service is busy right now. Please try again in a minute.";
                    continue;
                }
                // 400/403/404 = our request or key is wrong; another model won't help
                log.error("Gemini rejected the request ({}): {}", status, e.getResponseBodyAsString());
                return "Error communicating with AI service (HTTP " + status + ").";
            } catch (ResourceAccessException e) {
                log.warn("Model {} timed out, trying next model", model);
                lastError = "The AI service timed out. Please try again.";
            }
        }

        return lastError;
    }

    private static String extractText(Map<?, ?> body) {
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
        return null;
    }
}