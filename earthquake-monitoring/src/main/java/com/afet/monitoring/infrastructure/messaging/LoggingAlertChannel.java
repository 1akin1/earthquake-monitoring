package com.afet.monitoring.infrastructure.messaging;

import org.springframework.context.annotation.Profile;
import com.afet.monitoring.domain.service.alert.AlertChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The real {@link AlertChannel} for now: it "delivers" an alert by logging it and returns
 * a handle so the delivery can be retracted (also logged). Stands in for a future
 * SMS/push/siren integration — swapping in a real channel means one new implementation of
 * this port, with no change to any command. Pure infrastructure.
 */
@Profile("!platform") // consumer-side reaction; in the platform it lives in its own service
@Component
public class LoggingAlertChannel implements AlertChannel {

    private static final Logger log = LoggerFactory.getLogger(LoggingAlertChannel.class);

    private final AtomicLong sequence = new AtomicLong();

    @Override
    public String send(String message) {
        String handle = "alert-" + sequence.incrementAndGet();
        log.warn("[ALERT->SEND {}] {}", handle, message);
        return handle;
    }

    @Override
    public void retract(String handle) {
        log.warn("[ALERT->RETRACT {}] previous alert withdrawn", handle);
    }
}
