package com.afet.monitoring.domain.service.alert;

import com.afet.monitoring.domain.event.EarthquakeDetectedEvent;

import java.util.Locale;

/**
 * Concrete Command — pages the on-call responders for a HIGH/CRITICAL event. Undoable:
 * {@link #undo()} retracts the page (e.g. a "stand down" on a false alarm).
 */
public class NotifyRespondersCommand implements AlertCommand {

    private final AlertChannel channel;
    private final EarthquakeDetectedEvent event;
    private String handle; // delivery handle captured on execute, consumed by undo

    public NotifyRespondersCommand(AlertChannel channel, EarthquakeDetectedEvent event) {
        this.channel = channel;
        this.event = event;
    }

    @Override
    public void execute() {
        String message = String.format(Locale.US,
                "RESPONDERS: M%.1f %s near (%.4f, %.4f) from %s — dispatch teams",
                event.magnitude(), event.riskLevel(),
                event.latitude(), event.longitude(), event.source());
        handle = channel.send(message);
    }

    @Override
    public void undo() {
        if (handle != null) {
            channel.retract(handle);
            handle = null;
        }
    }

    @Override
    public String description() {
        return "notify responders (M" + String.format(Locale.US, "%.1f", event.magnitude()) + ")";
    }
}
