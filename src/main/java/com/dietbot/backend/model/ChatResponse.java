package com.dietbot.backend.model;

public class ChatResponse {
    private String responseText;

    public ChatResponse(String responseText) {
        this.responseText = responseText;
    }

    public String getResponseText() {
        return responseText;
    }
    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }
}