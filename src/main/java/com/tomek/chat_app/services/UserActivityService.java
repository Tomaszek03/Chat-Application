package com.tomek.chat_app.services;

import io.lettuce.core.api.sync.RedisCommands;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for tracking and updating user activity timestamps
 * for each chat room using Redis Sorted Sets (ZSET).
 * <p>
 * Each room has its own presence key in the format:
 * <pre>
 * presence:{roomId}
 * </pre>
 * Users are stored as members of the sorted set, and the score represents
 * the timestamp of their last recorded activity (in milliseconds).
 * </p>
 */
@Slf4j
@Singleton
public class UserActivityService {

    private final RedisCommands<String, String> redis;

    public UserActivityService(RedisChatService redisChatService) {
        this.redis = redisChatService.getSync();
    }

    /**
     * Marks the user as active in the given room by updating the timestamp
     * of their entry in the Redis ZSET. If the user does not exist in the set,
     * they are added automatically.
     *
     * <p>
     * Stored Redis structure:
     * <pre>
     * ZADD presence:{roomId} {timestamp} {username}
     * </pre>
     * </p>
     *
     * @param roomId   the room in which the user is active
     * @param username the username of the active user
     */
    public void markUserActive(String roomId, String username) {
        log.debug("User: {} marked as active", username);
        String key = "presence:" + roomId;
        long now = System.currentTimeMillis();
        redis.zadd(key, now, username);
    }

    /**
     * Removes the user from the presence ZSET of the specified room.
     * <p>
     * This is typically done when the WebSocket connection closes or when
     * the user is detected as inactive by the {@link InactivityChecker}.
     * </p>
     *
     * <p>
     * Redis command:
     * <pre>
     * ZREM presence:{roomId} {username}
     * </pre>
     * </p>
     *
     * @param roomId   the room from which to remove the user
     * @param username the username to remove
     */
    public void removeUser(String roomId, String username) {
        redis.zrem("presence:" + roomId, username);
    }
}
