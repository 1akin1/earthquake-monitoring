package com.afet.alert.command;

import com.afet.platform.events.EarthquakeDetectedEvent;
import java.util.Locale;

/** Concrete Command — public broadcast for CRITICAL; undo issues a stand-down. */
public class BroadcastPublicWarningCommand implements AlertCommand {
    private final AlertChannel channel;
    private final EarthquakeDetectedEvent event;
    private String handle;

    public BroadcastPublicWarningCommand(AlertChannel channel, EarthquakeDetectedEvent event) {
        this.channel = channel; this.event = event;
    }
    @Override public void execute() {
        handle = channel.send(String.format(Locale.US,
                "PUBLIC WARNING: strong earthquake M%.1f near (%.4f, %.4f) — take cover now",
                event.magnitude(), event.latitude(), event.longitude()));
    }
    @Override public void undo() { if (handle != null) { channel.retract(handle); handle = null; } }
    @Override public String description() {
        return "broadcast public warning (M" + String.format(Locale.US, "%.1f", event.magnitude()) + ")";
    }
}
