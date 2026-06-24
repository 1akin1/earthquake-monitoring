package com.afet.monitoring.infrastructure.messaging;

import com.afet.monitoring.domain.event.EarthquakeDetectedEvent;
import com.afet.monitoring.domain.port.EarthquakeEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Driven adapter: implements the domain {@link EarthquakeEventPublisherPort} using
 * Kafka. The rest of the app never sees {@code KafkaTemplate}. Keying by earthquake id
 * routes all messages for one quake to the same partition (ordering guarantee).
 */
@Component
public class KafkaEarthquakeEventPublisher implements EarthquakeEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaEarthquakeEventPublisher.class);

    private final KafkaTemplate<String, EarthquakeDetectedEvent> kafkaTemplate;

    public KafkaEarthquakeEventPublisher(KafkaTemplate<String, EarthquakeDetectedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishDetected(EarthquakeDetectedEvent event) {
        String key = String.valueOf(event.id());
        kafkaTemplate.send(KafkaTopics.EARTHQUAKE_DETECTED, key, event);
        log.info("Published {} to topic {} (key={})",
                EarthquakeDetectedEvent.class.getSimpleName(), KafkaTopics.EARTHQUAKE_DETECTED, key);
    }
}
