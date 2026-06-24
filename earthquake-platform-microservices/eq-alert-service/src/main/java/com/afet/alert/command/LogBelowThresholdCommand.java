package com.afet.alert.command;

import com.afet.platform.events.EarthquakeDetectedEvent;
import java.util.Locale;

/** Concrete Command — records a low-risk note; undo is intentionally a no-op. */
public class LogBelowThresholdCommand implements AlertCommand {
    private final AlertChannel channel;
    private final EarthquakeDetectedEvent event;

    public LogBelowThresholdCommand(AlertChannel channel, EarthquakeDetectedEvent event) {
        this.channel = channel; this.event = event;
    }
    @Override public void execute() {
        channel.send(String.format(Locale.US,
                "NOTE: M%.1f %s below alert threshold — recorded, no notification",
                event.magnitude(), event.riskLevel()));
    }
    @Override public void undo() { /* no-op: an informational record is not retracted */ }
    @Override public String description() {
        return "log below-threshold note (M" + String.format(Locale.US, "%.1f", event.magnitude()) + ")";
    }
}
