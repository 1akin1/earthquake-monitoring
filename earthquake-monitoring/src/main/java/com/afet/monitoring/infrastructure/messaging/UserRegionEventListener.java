package com.afet.monitoring.infrastructure.messaging;

import org.springframework.context.annotation.Profile;
import com.afet.monitoring.domain.event.EarthquakeDetectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Observer #3 — the user-region reaction. Logs which region was affected so users in
 * that area could later be looked up. Again its own {@code groupId}. (Stands in for a
 * future user-service microservice.)
 */
@Profile("!platform") // consumer-side reaction; in the platform it lives in its own service
@Component
public class UserRegionEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserRegionEventListener.class);

    @KafkaListener(topics = KafkaTopics.EARTHQUAKE_DETECTED, groupId = "user-service")
    public void onEarthquakeDetected(EarthquakeDetectedEvent event) {
        log.info("[USER] event near ({}, {}) — flagging users within affected region.",
                event.latitude(), event.longitude());
    }
}
