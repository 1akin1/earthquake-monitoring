package com.afet.alert.channel;

import com.afet.alert.command.AlertChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

/** Logs the alert and returns a handle so it can be retracted. Swap for SMS/push later. */
@Component
public class LoggingAlertChannel implements AlertChannel {
    private static final Logger log = LoggerFactory.getLogger(LoggingAlertChannel.class);
    private final AtomicLong sequence = new AtomicLong();
    @Override public String send(String message) {
        String handle = "alert-" + sequence.incrementAndGet();
        log.warn("[ALERT->SEND {}] {}", handle, message);
        return handle;
    }
    @Override public void retract(String handle) {
        log.warn("[ALERT->RETRACT {}] previous alert withdrawn", handle);
    }
}
