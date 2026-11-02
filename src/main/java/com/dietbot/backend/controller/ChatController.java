package com.dietbot.backend.controller;

import com.dietbot.backend.model.ChatRequest;
import com.dietbot.backend.model.ChatResponse;
import com.dietbot.backend.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private GeminiService geminiService;

    @PostMapping("/generate")
    public ChatResponse generateDietPlan(@RequestBody ChatRequest request) {
    	String aiResponse = geminiService.generateDietPlan(request);
    	return new ChatResponse(aiResponse);
    }
}