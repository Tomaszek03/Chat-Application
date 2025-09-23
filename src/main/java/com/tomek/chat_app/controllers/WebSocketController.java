package com.tomek.chat_app.controllers;

import com.tomek.chat_app.services.RedisChatService;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import jakarta.inject.Inject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ServerWebSocket("/{roomId}")
public class WebSocketController {
    private final ConcurrentMap<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final RedisChatService redisChatService;

    @Inject
    public WebSocketController(RedisChatService redisChatService) {
        this.redisChatService = redisChatService;
    }

    @OnOpen
    public void onOpen(WebSocketSession session, String roomId) {
        String username = session.getRequestParameters()
                .getFirst("username")
                .orElse("anonymous");

        rooms.computeIfAbsent(roomId, r -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @OnMessage
    public void onMessage(WebSocketSession session, String message, String roomId) {
        String username = session.getRequestParameters()
                .getFirst("username")
                .orElse("anonymous");
        String id = redisChatService.saveMessage(roomId, username, message);
        String json =  String.format("{\"id\":\"%s\",\"username\":\"%s\",\"messageContent\":\"%s\",\"ts\":%d}", id, username, message, System.currentTimeMillis());

        broadCastToRoom(roomId, json, null);
    }

    @OnClose
    public void onClose(WebSocketSession session, String roomId) {
        Set<WebSocketSession> set = rooms.get(roomId);
        if (set != null) {
            set.remove(session);
        }
    }

    private void broadCastToRoom(String roomId, String json, WebSocketSession session) {
        Set<WebSocketSession> set = rooms.get(roomId);
        if (set == null) return;
        for(WebSocketSession s : set) {
            if(s.isOpen() && !s.equals(session)) {
                try {
                    s.sendSync(json);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
