package com.afet.alert.command;

import com.afet.platform.events.EarthquakeDetectedEvent;
import java.util.Locale;

/** Concrete Command — pages responders for HIGH/CRITICAL; undo retracts the page. */
public class NotifyRespondersCommand implements AlertCommand {
    private final AlertChannel channel;
    private final EarthquakeDetectedEvent event;
    private String handle;

    public NotifyRespondersCommand(AlertChannel channel, EarthquakeDetectedEvent event) {
        this.channel = channel; this.event = event;
    }
    @Override public void execute() {
        handle = channel.send(String.format(Locale.US,
                "RESPONDERS: M%.1f %s near (%.4f, %.4f) from %s — dispatch teams",
                event.magnitude(), event.riskLevel(), event.latitude(), event.longitude(), event.source()));
    }
    @Override public void undo() { if (handle != null) { channel.retract(handle); handle = null; } }
    @Override public String description() {
        return "notify responders (M" + String.format(Locale.US, "%.1f", event.magnitude()) + ")";
    }
}
