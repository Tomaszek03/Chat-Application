package com.tomek.chat_app.services;

import io.micronaut.websocket.WebSocketSession;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Service responsible for managing WebSocket sessions and broadcasting messages to rooms.
 * <p>
 * Maintains a mapping of room IDs to active WebSocket sessions. Provides methods to add
 * and remove sessions from rooms, broadcast messages to all active sessions in a room,
 * and query active rooms or sessions.
 * </p>
 */
@Slf4j
@Singleton
public class WebSocketBroadcastService {

    /**
     * Mapping from room IDs to sets of active WebSocket sessions in that room.
     */
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    /**
     * Adds a WebSocket session to the specified room.
     * If the room does not exist, it will be created.
     *
     * @param roomId  the identifier of the chat room
     * @param session the WebSocket session to add
     */
    public void addSession(String roomId, WebSocketSession session) {
        rooms.computeIfAbsent(roomId, r -> ConcurrentHashMap.newKeySet()).add(session);
    }

    /**
     * Removes a WebSocket session from the specified room.
     * If the room does not exist, this method does nothing.
     *
     * @param roomId  the identifier of the chat room
     * @param session the WebSocket session to remove
     */
    public void removeSession(String roomId, WebSocketSession session) {
        Set<WebSocketSession> sessions = rooms.get(roomId);
        if (sessions != null) sessions.remove(session);
    }

    /**
     * Broadcasts a JSON message to all open WebSocket sessions in the specified room.
     * If the room has no sessions, a warning is logged and nothing is sent.
     *
     * @param roomId the identifier of the chat room
     * @param json   the JSON-formatted message to send
     */
    public void broadcast(String roomId, String json) {
        log.debug("Broadcasting to room: {}", roomId);

        Set<WebSocketSession> sessions = rooms.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            log.warn("No sessions in room: {}", roomId);
            return;
        }

        for(WebSocketSession session : sessions) {
            if(session.isOpen()) {
                try {
                    session.sendSync(json);
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Returns the set of currently active room IDs.
     *
     * @return a set of active room identifiers
     */
    public Set<String> getActiveRooms() {
        return rooms.keySet();
    }

    /**
     * Returns the set of WebSocket sessions in the specified room.
     * If the room does not exist, returns an empty set.
     *
     * @param roomId the identifier of the chat room
     * @return a set of WebSocket sessions in the room
     */
    public Set<WebSocketSession> getSessions(String roomId) {
        return rooms.getOrDefault(roomId, Collections.emptySet());
    }
}
