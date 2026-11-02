package com.dietbot.backend.model;

public class ChatRequest {
    private String prompt;
    private String dietaryPreferences;
    private int targetCalories;

    // Getters and Setters
    public String getPrompt() {
        return prompt;
    }
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    public String getDietaryPreferences() {
        return dietaryPreferences;
    }
    public void setDietaryPreferences(String dietaryPreferences) {
        this.dietaryPreferences = dietaryPreferences;
    }
    public int getTargetCalories() {
        return targetCalories;
    }
    public void setTargetCalories(int targetCalories) {
        this.targetCalories = targetCalories;
    }
}