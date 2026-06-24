package com.afet.monitoring.infrastructure.messaging;

/** Centralised Kafka topic names. Compile-time constants so they can be used in @KafkaListener. */
public final class KafkaTopics {
    private KafkaTopics() {}
    public static final String EARTHQUAKE_DETECTED = "earthquake.detected";
}
