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
import lombok.Getter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for storing, retrieving, and clearing chat messages
 * using Redis Streams. Each chat room is mapped to a Redis Stream
 * with a key in the format <code>chat:{roomId}</code>.
 * <p>
 * The service uses Lettuce for Redis communication and maintains a
 * single synchronous Redis connection that is automatically closed
 * when the application shuts down.
 * </p>
 */
@Singleton
public class RedisChatService {

    @Getter
    private final RedisCommands<String, String> sync;
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;

    /**
     * Creates a Redis-backed chat service and establishes a connection
     * to the Redis server. The URI is resolved from:
     * <ol>
     *     <li>System property <code>redis.uri</code></li>
     *     <li>Environment variable <code>REDIS_URI</code></li>
     *     <li>Default value <code>redis://localhost:6379</code></li>
     * </ol>
     */
    @Inject
    public RedisChatService() {
        String uri = System.getProperty("redis.uri", System.getenv().getOrDefault("REDIS_URI", "redis://localhost:6379"));
        this.redisClient = RedisClient.create(uri);
        this.connection = redisClient.connect();
        this.sync = connection.sync();
    }

    /**
     * Saves a chat message to a Redis Stream corresponding to the given room.
     *
     * @param roomId   the room in which the message was sent
     * @param username the author of the message
     * @param message  the message content
     * @return the automatically generated Redis Stream entry ID
     */
    public String saveMessage(String roomId, String username, String message) {
        String streamKey = "chat:" + roomId;
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("message", message);
        body.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return sync.xadd(streamKey, body);
    }

    /**
     * Retrieves all messages from the Redis Stream for the given room.
     * <p>
     * Each Redis Stream entry is mapped into a {@link Message} DTO.
     * </p>
     *
     * @param roomId the target chat room
     * @return a list of messages in chronological order
     */
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

    /**
     * Deletes the entire Redis Stream for the specified room.
     * <p>
     * This effectively clears all chat history for the room.
     * If the stream does not exist, the operation is silently ignored.
     * </p>
     *
     * @param roomId the room whose message history should be deleted
     */
    public void clearMessages(String roomId) {
        String streamKey = "chat:" + roomId;
        sync.del(streamKey);
    }

    /**
     * Closes the Redis connection and shuts down the Redis client
     * when the application context is being destroyed.
     */
    @PreDestroy
    public void shutdown() {
        connection.close();
        redisClient.shutdown();
    }
}
