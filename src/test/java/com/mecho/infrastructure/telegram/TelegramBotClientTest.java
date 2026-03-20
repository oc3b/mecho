package com.mecho.infrastructure.telegram;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramBotClientTest {

    @Test
    void shouldParseKeyboardPayload() throws Exception {
        TelegramBotClient client = new TelegramBotClient(null, "test-token");
        
        java.lang.reflect.Method method = TelegramBotClient.class.getDeclaredMethod("buildKeyboardPayload", String.class, String.class, String[][].class);
        method.setAccessible(true);
        
        String[][] keyboard = {{"Button1"}};
        String payload = (String) method.invoke(client, "chat", "message", keyboard);
        
        assertThat(payload).contains("\"chat_id\":\"chat\"");
        assertThat(payload).contains("\"text\":\"message\"");
    }

    @Test
    void shouldEscapeJson() throws Exception {
        TelegramBotClient client = new TelegramBotClient(null, "test-token");
        
        java.lang.reflect.Method method = TelegramBotClient.class.getDeclaredMethod("escapeJson", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(client, "test\"quote");
        
        assertThat(result).contains("\\\"");
        assertThat(result).contains("test\\\"quote");
    }
}
