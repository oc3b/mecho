package com.mecho.domain.alert;

import com.mecho.domain.model.AlertStatus;
import com.mecho.domain.prediction.Prediction;
import java.time.Instant;

public record Alert(
    Long id,
    Prediction prediction,
    Instant sentAt,
    String channel,
    AlertStatus status
) {
    public Alert {
        if (sentAt == null) sentAt = Instant.now();
        if (channel == null) channel = "TELEGRAM";
        if (status == null) status = AlertStatus.PENDING;
    }
    
    public static Alert create(Prediction prediction, String channel) {
        return new Alert(null, prediction, Instant.now(), channel, AlertStatus.PENDING);
    }
    
    public Alert markSent() {
        return new Alert(id, prediction, Instant.now(), channel, AlertStatus.SENT);
    }
    
    public Alert markFailed() {
        return new Alert(id, prediction, sentAt, channel, AlertStatus.FAILED);
    }
}
