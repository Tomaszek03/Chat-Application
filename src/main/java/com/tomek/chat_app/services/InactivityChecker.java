package com.tomek.chat_app.services;

import io.lettuce.core.Range;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.Set;

/**
 * Periodically checks user activity across all active chat rooms and detects users
 * who have been inactive for longer than a configured threshold.
 * <p>
 * This service uses:
 * <ul>
 *     <li><strong>Redis Sorted Sets (ZSET)</strong> — to store user last-active timestamps</li>
 *     <li><strong>Reactor Flux</strong> — to run a non-blocking repeating task</li>
 *     <li><strong>WebSocketBroadcastService</strong> — to notify other users in the room</li>
 * </ul>
 * </p>
 *
 * <p>
 * The checker starts automatically on application startup thanks to {@link Context},
 * which eagerly initializes the bean even if it is not directly referenced.
 * It periodically scans all rooms at an interval defined by:
 * <ul>
 *     <li><code>inactivity.threshold-seconds</code> — how long a user may be inactive</li>
 *     <li><code>inactivity.check-interval-seconds</code> — how often to run the check</li>
 * </ul>
 * </p>
 */
@Slf4j
@Singleton
@Context
public class InactivityChecker {

    private final RedisCommands<String, String> redis;
    private final WebSocketBroadcastService broadcaster;
    private final int thresholdSeconds;
    private final int checkIntervalSeconds;

    public InactivityChecker(RedisChatService redisChatService, WebSocketBroadcastService broadcaster,
                             @Value("${inactivity.threshold-seconds}") int thresholdSeconds,
                             @Value("${inactivity.check-interval-seconds}") int checkIntervalSeconds) {
        this.redis = redisChatService.getSync();
        this.broadcaster = broadcaster;
        this.thresholdSeconds = thresholdSeconds;
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    /**
     * Starts a periodic non-blocking Reactor task that triggers every
     * {@code checkIntervalSeconds}. Each tick scans all active rooms
     * and determines which users are inactive.
     */
    @PostConstruct
    public void start() {
        log.info("Starting InactivityChecker...");
        Flux.interval(Duration.ofSeconds(checkIntervalSeconds))
                .subscribe(tick -> checkAllRooms());
    }

    /**
     * Checks all active rooms and identifies users whose last activity timestamp
     * is older than the configured inactivity threshold.
     * <p>
     * A user is considered inactive if:
     * <pre>
     * lastActiveTimestamp <= currentTime - thresholdSeconds
     * </pre>
     * </p>
     *
     * <p>
     * The method:
     * <ol>
     *     <li>Fetches all active rooms</li>
     *     <li>For each room, loads users with scores below the cutoff time</li>
     *     <li>Broadcasts an <code>userInactive</code> event via WebSocket</li>
     *     <li>Removes inactive users from Redis for cleanup</li>
     * </ol>
     * </p>
     */
    private void checkAllRooms() {
        Set<String> rooms = broadcaster.getActiveRooms();
        long cutoff = System.currentTimeMillis() - thresholdSeconds * 1000L;

        for(String roomId : rooms) {
            String key = "presence:" + roomId;

            redis.zrangebyscore(key, Range.create(Double.NEGATIVE_INFINITY, cutoff))
                    .forEach(inactiveUser -> {
                        log.debug("User: {} is inactive in room: {}", inactiveUser, roomId);

                        broadcaster.broadcast(roomId,
                                "{\"type\": \"userInactive\", " +
                                "\"user\": \"" + inactiveUser + "\" }"
                        );

                        redis.zrem(key, inactiveUser);
                    });

        }
    }
}
