package com.tomek.chat_app.services;

import com.tomek.chat_app.dto.Message;
import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class RedisChatService {
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> sync;

    @Inject
    public RedisChatService() {
        String uri = System.getProperty("redis.uri", System.getenv().getOrDefault("REDIS_URI", "redis://localhost:6379"));
        this.redisClient = RedisClient.create(uri);
        this.connection = redisClient.connect();
        this.sync = connection.sync();
    }

    public String saveMessage(String roomId, String username, String message) {
        String streamKey = "chat:" + roomId;
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("message", message);
        body.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return sync.xadd(streamKey, body);
    }

    public List<Message> getMessages(String roomId) {
        String streamKey = "chat:" + roomId;
        List<StreamMessage<String,String>> entries =
                sync.xrange(streamKey, Range.from(Range.Boundary.unbounded(), Range.Boundary.unbounded()));
        List<Message> messages = new ArrayList<>();
        for(StreamMessage<String,String> entry : entries) {
            Map<String,String> body = entry.getBody();
            Message message = new Message();
            message.setId(entry.getId());
            message.setUsername(body.get("username"));
            message.setMessageContent(body.get("message"));
            message.setTimestamp(Long.parseLong(body.getOrDefault("timestamp","0")));
            messages.add(message);
        }
        return messages;
    }

    @PreDestroy
    public void shutdown() {
        connection.close();
        redisClient.shutdown();
    }
}
