package com.tomek.chat_app.controllers;

import com.tomek.chat_app.dto.Message;
import com.tomek.chat_app.services.RedisChatService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.inject.Inject;

import java.util.List;

@Controller("/chat")
public class ChatController {
    @Inject
    RedisChatService redisChatService;

    @Get("/{roomId}")
    public List<String> history(String roomId) {
        return redisChatService.getMessages(roomId).stream()
                .map(msg -> {
                    String time = java.time.Instant.ofEpochMilli(msg.getTimestamp())
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime()
                            .toString();
                    return String.format("[%s] %s: %s", time, msg.getUsername(), msg.getMessageContent());
                })
                .toList();
    }
}
