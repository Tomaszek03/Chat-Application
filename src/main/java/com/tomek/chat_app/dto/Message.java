package com.tomek.chat_app.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

/**
 * Data Transfer Object representing a single chat message stored in Redis Streams.
 * <p>
 * A message contains metadata such as:
 * <ul>
 *     <li>The Redis Stream entry ID</li>
 *     <li>The username of the sender</li>
 *     <li>The message body</li>
 *     <li>A client-side timestamp indicating when the message was created</li>
 * </ul>
 * </p>
 * <p>
 * The DTO is annotated with {@link Serdeable} to enable Micronaut serialization
 * for WebSocket communication and REST responses.
 * </p>
 */
@Data
@Serdeable
public class Message {
    private String id;
    private String username;
    private String messageContent;
    private long timestamp;
}
