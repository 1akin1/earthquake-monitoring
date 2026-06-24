package com.afet.user.consumer;

import com.afet.platform.events.EarthquakeDetectedEvent;
import com.afet.platform.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** The user-region reaction, now its own service. groupId "user-service" (unchanged). */
@Component
public class UserRegionEventListener {
    private static final Logger log = LoggerFactory.getLogger(UserRegionEventListener.class);

    @KafkaListener(topics = Topics.EARTHQUAKE_DETECTED, groupId = "user-service")
    public void onEarthquakeDetected(EarthquakeDetectedEvent event) {
        log.info("[USER] event near ({}, {}) — flagging users within affected region.",
                event.latitude(), event.longitude());
    }
}
