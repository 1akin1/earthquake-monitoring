package com.afet.monitoring.domain.service.alert;

import com.afet.monitoring.domain.event.EarthquakeDetectedEvent;

import java.util.Locale;

/**
 * Concrete Command — pushes a public emergency broadcast for a CRITICAL event. Undoable:
 * {@link #undo()} issues a stand-down so a mistaken public warning can be withdrawn.
 */
public class BroadcastPublicWarningCommand implements AlertCommand {

    private final AlertChannel channel;
    private final EarthquakeDetectedEvent event;
    private String handle;

    public BroadcastPublicWarningCommand(AlertChannel channel, EarthquakeDetectedEvent event) {
        this.channel = channel;
        this.event = event;
    }

    @Override
    public void execute() {
        String message = String.format(Locale.US,
                "PUBLIC WARNING: strong earthquake M%.1f near (%.4f, %.4f) — take cover now",
                event.magnitude(), event.latitude(), event.longitude());
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
        return "broadcast public warning (M" + String.format(Locale.US, "%.1f", event.magnitude()) + ")";
    }
}
