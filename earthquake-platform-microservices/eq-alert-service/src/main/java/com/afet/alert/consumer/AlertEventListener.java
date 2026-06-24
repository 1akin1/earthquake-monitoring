package com.afet.alert.consumer;

import com.afet.alert.command.*;
import com.afet.platform.events.EarthquakeDetectedEvent;
import com.afet.platform.events.RiskLevel;
import com.afet.platform.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The alert reaction, now its OWN service. Consumes earthquake.detected with groupId
 * "alert-service" (unchanged from the monolith, so the offsets/semantics carry over) and
 * is the Command pattern's client: builds the commands for the event's risk level and
 * hands them to the dispatcher for retry + rollback.
 */
@Component
public class AlertEventListener {

    private static final Logger log = LoggerFactory.getLogger(AlertEventListener.class);

    private final AlertChannel alertChannel;
    private final AlertDispatcher dispatcher;

    public AlertEventListener(AlertChannel alertChannel, AlertDispatcher dispatcher) {
        this.alertChannel = alertChannel;
        this.dispatcher = dispatcher;
    }

    @KafkaListener(topics = Topics.EARTHQUAKE_DETECTED, groupId = "alert-service")
    public void onEarthquakeDetected(EarthquakeDetectedEvent event) {
        try {
            dispatcher.dispatchBatch(commandsFor(event));
        } catch (AlertDispatchException ex) {
            log.error("[ALERT] dispatch failed and was rolled back: {}", ex.getMessage());
        }
    }

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
