package com.tomek.chat_app.controllers;

import com.tomek.chat_app.services.RedisChatService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.time.*;

import java.util.List;

/**
 * Controller that exposes HTTP endpoints for retrieving and deleting
 * chat history stored in Redis Streams for specific chat rooms.
 */
@Slf4j
@Controller("/chat")
public class ChatHistoryController {
    @Inject
    RedisChatService redisChatService;

    /**
     * Retrieves the chat history for a specified room.
     * <p>
     * Messages are fetched from Redis and transformed into
     * human-readable string format containing timestamp,
     * username, and message content.
     * </p>
     *
     * @param roomId the identifier of the chat room
     * @return a list of formatted chat messages belonging to the room
     */
    @Get("/{roomId}")
    public List<String> getChatHistory(String roomId) {
        log.info("Retrieved chat history from room: {} ", roomId);
        return redisChatService.getMessages(roomId).stream()
                .map(msg -> {
                    String time = Instant.ofEpochMilli(msg.getTimestamp())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                            .toString();
                    return String.format("[%s] %s: %s", time, msg.getUsername(), msg.getMessageContent());
                })
                .toList();
    }

    /**
     * Deletes the chat history for a specified room.
     * <p>
     * All message IDs in the Redis Stream belonging to the room
     * are removed. If the stream is empty, the operation is a no-op.
     * </p>
     *
     * @param roomId the identifier of the room whose history should be cleared
     * @return HTTP 204 No Content if deletion is successful
     */
    @Delete("/{roomId}")
    public HttpResponse<Void> clearChatHistory(String roomId) {
        redisChatService.clearMessages(roomId);
        log.info("Chat history from room: {} has been deleted", roomId);
        return HttpResponse.noContent();
    }
}
