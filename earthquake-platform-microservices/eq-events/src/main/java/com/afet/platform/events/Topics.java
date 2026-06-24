package com.afet.platform.events;

/** Kafka topic names shared by the producer and every consumer service. */
public final class Topics {
    private Topics() {}
    public static final String EARTHQUAKE_DETECTED = "earthquake.detected";
}
