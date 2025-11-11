package com.tomek.chat_app.controllers;

import com.tomek.chat_app.services.UserActivityService;
import com.tomek.chat_app.services.RedisChatService;
import com.tomek.chat_app.services.WebSocketBroadcastService;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket controller for chat rooms.
 *
 * <p>
 * Manages WebSocket connections for chat rooms identified by {@code roomId}.
 * Handles user connections, disconnections, sending messages, and presence tracking.
 * </p>
 *
 * <p>
 * Uses {@link UserActivityService} to track user activity and
 * {@link WebSocketBroadcastService} to broadcast messages to all connected sessions
 * in the same room.
 * </p>
 */
@Slf4j
@ServerWebSocket("/{roomId}")
public class WebSocketController {

    @Inject private UserActivityService userActivityService;
    @Inject private WebSocketBroadcastService broadcaster;

    private final RedisChatService redisChatService;

    @Inject
    public WebSocketController(RedisChatService redisChatService) {
        this.redisChatService = redisChatService;
    }

    /**
     * Called when a new WebSocket session is opened.
     *
     * <p>
     * Adds the session to the broadcaster, marks the user as active in presence tracking,
     * and logs the event.
     * </p>
     *
     * @param session the WebSocket session that just connected
     * @param roomId  the chat room ID
     */
    @OnOpen
    public void onOpen(WebSocketSession session, String roomId) {
        String username = session.getRequestParameters()
                .getFirst("username")
                .orElse("anonymous");

        broadcaster.addSession(roomId, session);
        userActivityService.markUserActive(roomId, username);

        log.info("User: {} connected to room: {}", username, roomId);

        int sessionCount = broadcaster.getSessions(roomId).size();
        log.debug("Sessions in room {}: {}", roomId, sessionCount);
    }

    /**
     * Called when a message is received from a WebSocket session.
     *
     * <p>
     * Saves the message in Redis, broadcasts it to other sessions in the room, and updates
     * user presence.
     * </p>
     *
     * @param session the WebSocket session sending the message
     * @param message the message content
     * @param roomId  the chat room ID
     */
    @OnMessage
    public void onMessage(WebSocketSession session, String message, String roomId) {
        String username = session.getRequestParameters()
                .getFirst("username")
                .orElse("anonymous");
        String id = redisChatService.saveMessage(roomId, username, message);
        String json =  String.format("{\"id\":\"%s\",\"username\":\"%s\",\"messageContent\":\"%s\",\"ts\":%d}", id, username, message, System.currentTimeMillis());

        broadcaster.broadcast(roomId, json);
        userActivityService.markUserActive(roomId, username);

        log.info("User: {} sent message to room: {}", username, roomId);
    }

    /**
     * Called when a WebSocket session is closed.
     *
     * <p>
     * Removes the session from the broadcaster, removes the user from presence tracking,
     * and logs the disconnection.
     * </p>
     *
     * @param session the WebSocket session that disconnected
     * @param roomId  the chat room ID
     */
    @OnClose
    public void onClose(WebSocketSession session, String roomId) {
        String username = session.getRequestParameters()
                .getFirst("username")
                .orElse("anonymous");


        broadcaster.removeSession(roomId, session);
        userActivityService.removeUser(roomId, username);

        log.info("User: {} disconnected from room: {}", username, roomId);
    }
}
