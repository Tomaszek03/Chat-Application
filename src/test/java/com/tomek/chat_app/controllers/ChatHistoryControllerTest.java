package com.tomek.chat_app.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import com.tomek.chat_app.dto.Message;
import com.tomek.chat_app.services.RedisChatService;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

class ChatHistoryControllerTest {
    @Test
    void messagesFormattingTest() {
        List<Message> messages = new ArrayList<>();
        Message message1 = new Message();
        message1.setUsername("user1");
        message1.setMessageContent("message1");
        message1.setTimestamp(1695477741000L);
        messages.add(message1);

        Message message2 = new Message();
        message2.setUsername("user2");
        message2.setMessageContent("message2");
        message2.setTimestamp(1695477805000L);
        messages.add(message2);

        RedisChatService mockRedisChatService = new RedisChatService() {
            @Override
            public List<Message> getMessages(String username) {
                return messages;
            }
        };

        ChatHistoryController controller = new ChatHistoryController();
        controller.redisChatService = mockRedisChatService;

        List<String> result = controller.getChatHistory("room1");

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).contains("[").contains("user1:").contains("message1");
        assertThat(result.get(1)).contains("[").contains("user2:").contains("message2");
    }
}