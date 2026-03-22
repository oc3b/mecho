package com.mecho.infrastructure.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Component
public class TelegramBotClient {
    
    private static final Logger log = LoggerFactory.getLogger(TelegramBotClient.class);
    
    private final WebClient webClient;
    private final String botToken;
    private final String apiUrl;
    
    @Value("${mecho.telegram.api-url:https://api.telegram.org}")
    private String configuredApiUrl;

    public TelegramBotClient(@Qualifier("telegramWebClient") WebClient webClient,
                            @Value("${mecho.telegram.bot-token:}") String botToken) {
        this.webClient = webClient;
        this.botToken = botToken;
        this.apiUrl = "https://api.telegram.org";
    }

    public CompletableFuture<Boolean> sendAlert(String chatId, String message) {
        if (botToken == null || botToken.isBlank()) {
            log.warn("Telegram bot token not configured, skipping alert");
            return CompletableFuture.completedFuture(false);
        }
        
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/bot" + botToken + "/sendMessage")
                        .queryParam("chat_id", chatId)
                        .queryParam("text", message)
                        .queryParam("parse_mode", "Markdown")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableError))
                .timeout(Duration.ofSeconds(30))
                .map(response -> {
                    log.debug("Telegram message sent successfully to {}", chatId);
                    return true;
                })
                .onErrorResume(e -> {
                    log.error("Failed to send Telegram message to {}: {}", chatId, e.getMessage());
                    return Mono.just(false);
                })
                .toFuture();
    }

    public CompletableFuture<Boolean> sendAlertWithKeyboard(String chatId, String message, 
                                                            String[][] keyboard) {
        if (botToken == null || botToken.isBlank()) {
            log.warn("Telegram bot token not configured, skipping alert");
            return CompletableFuture.completedFuture(false);
        }
        
        return webClient.post()
                .uri("/bot" + botToken + "/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildKeyboardPayload(chatId, message, keyboard))
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableError))
                .timeout(Duration.ofSeconds(30))
                .map(response -> {
                    log.debug("Telegram message with keyboard sent to {}", chatId);
                    return true;
                })
                .onErrorResume(e -> {
                    log.error("Failed to send Telegram message to {}: {}", chatId, e.getMessage());
                    return Mono.just(false);
                })
                .toFuture();
    }

    public CompletableFuture<Boolean> setWebhook(String webhookUrl) {
        if (botToken == null || botToken.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/bot" + botToken + "/setWebhook")
                        .queryParam("url", webhookUrl)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    log.info("Telegram webhook set to {}", webhookUrl);
                    return true;
                })
                .onErrorResume(e -> {
                    log.error("Failed to set webhook: {}", e.getMessage());
                    return Mono.just(false);
                })
                .toFuture();
    }

    private String buildKeyboardPayload(String chatId, String message, String[][] keyboard) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"chat_id\":\"").append(chatId).append("\",");
        json.append("\"text\":\"").append(escapeJson(message)).append("\",");
        json.append("\"parse_mode\":\"Markdown\",");
        json.append("\"reply_markup\":{");
        json.append("\"inline_keyboard\":[");
        
        for (int i = 0; i < keyboard.length; i++) {
            json.append("[");
            for (int j = 0; j < keyboard[i].length; j++) {
                json.append("{\"text\":\"").append(keyboard[i][j]).append("\",\"callback_data\":\"").append(keyboard[i][j]).append("\"}");
                if (j < keyboard[i].length - 1) json.append(",");
            }
            json.append("]");
            if (i < keyboard.length - 1) json.append(",");
        }
        
        json.append("]}");
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wcre) {
            int status = wcre.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return false;
    }
}
