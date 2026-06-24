package com.afet.monitoring.domain.service.alert;

import com.afet.monitoring.domain.event.EarthquakeDetectedEvent;

import java.util.Locale;

/**
 * Concrete Command — records a low-risk event below the alert threshold. Deliberately
 * <b>not</b> undoable: an informational note that was written can't meaningfully be
 * "unwritten", so {@link #undo()} is a documented no-op. Shows that a command can opt out
 * of reversal while still fitting the same interface.
 */
public class LogBelowThresholdCommand implements AlertCommand {

    private final AlertChannel channel;
    private final EarthquakeDetectedEvent event;

    public LogBelowThresholdCommand(AlertChannel channel, EarthquakeDetectedEvent event) {
        this.channel = channel;
        this.event = event;
    }

    @Override
    public void execute() {
        String message = String.format(Locale.US,
                "NOTE: M%.1f %s below alert threshold — recorded, no notification",
                event.magnitude(), event.riskLevel());
        channel.send(message);
    }

    @Override
    public void undo() {
        // no-op: an informational record is not retracted
    }

    @Override
    public String description() {
        return "log below-threshold note (M" + String.format(Locale.US, "%.1f", event.magnitude()) + ")";
    }
}
