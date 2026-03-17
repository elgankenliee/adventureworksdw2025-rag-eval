package com.elgan.rag_eval.controller;

import com.elgan.rag_eval.model.ChatResponse;
import com.elgan.rag_eval.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody String message) {
        return chatService.chat(message);
    }

    @PostMapping("/retrieve")
    public String retrieve(@RequestBody String message) {
        return chatService.retrieveTables(message);
    }
}