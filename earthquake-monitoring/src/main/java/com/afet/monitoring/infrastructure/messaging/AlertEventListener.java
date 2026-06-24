package com.afet.monitoring.infrastructure.messaging;

import org.springframework.context.annotation.Profile;
import com.afet.monitoring.domain.event.EarthquakeDetectedEvent;
import com.afet.monitoring.domain.exception.AlertDispatchException;
import com.afet.monitoring.domain.model.RiskLevel;
import com.afet.monitoring.domain.service.alert.AlertChannel;
import com.afet.monitoring.domain.service.alert.AlertCommand;
import com.afet.monitoring.domain.service.alert.AlertDispatcher;
import com.afet.monitoring.domain.service.alert.BroadcastPublicWarningCommand;
import com.afet.monitoring.domain.service.alert.LogBelowThresholdCommand;
import com.afet.monitoring.domain.service.alert.NotifyRespondersCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Observer #1 — the alert reaction. Its own {@code groupId} means it receives EVERY event
 * independently of the other consumers (fan-out). (Stands in for a future alert-service
 * microservice.)
 *
 * <p>It is also the Command pattern's <b>client</b>: it doesn't perform alerts itself, it
 * builds the right {@link AlertCommand}s for the event's risk level and hands them to the
 * {@link AlertDispatcher}, which adds retry and all-or-nothing rollback. CRITICAL events
 * both page responders and broadcast publicly; HIGH pages responders; anything lower is
 * just recorded.
 */
@Profile("!platform") // consumer-side reaction; in the platform it lives in its own service
@Component
public class AlertEventListener {

    private static final Logger log = LoggerFactory.getLogger(AlertEventListener.class);

    private final AlertChannel alertChannel;
    private final AlertDispatcher dispatcher;

    public AlertEventListener(AlertChannel alertChannel, AlertDispatcher dispatcher) {
        this.alertChannel = alertChannel;
        this.dispatcher = dispatcher;
    }

    @KafkaListener(topics = KafkaTopics.EARTHQUAKE_DETECTED, groupId = "alert-service")
    public void onEarthquakeDetected(EarthquakeDetectedEvent event) {
        List<AlertCommand> commands = commandsFor(event);
        try {
            dispatcher.dispatchBatch(commands);
        } catch (AlertDispatchException ex) {
            // Retries exhausted; the batch was rolled back. Surface it for ops to chase.
            log.error("[ALERT] dispatch failed and was rolled back: {}", ex.getMessage());
        }
    }

    /** Choose the alert actions for an event's risk level. */
    private List<AlertCommand> commandsFor(EarthquakeDetectedEvent event) {
        List<AlertCommand> commands = new ArrayList<>();
        RiskLevel level = event.riskLevel();

        if (level == RiskLevel.HIGH || level == RiskLevel.CRITICAL) {
            commands.add(new NotifyRespondersCommand(alertChannel, event));
        }
        if (level == RiskLevel.CRITICAL) {
            commands.add(new BroadcastPublicWarningCommand(alertChannel, event));
        }
        if (commands.isEmpty()) {
            commands.add(new LogBelowThresholdCommand(alertChannel, event));
        }
        return commands;
    }
}
