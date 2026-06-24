package com.afet.monitoring.domain.port;

import com.afet.monitoring.domain.event.EarthquakeDetectedEvent;

/**
 * Outbound Port for publishing domain events. The application depends on THIS, not on
 * Kafka — so the messaging technology is swappable and the use cases stay testable
 * with a no-op/fake publisher (DIP). A Kafka adapter implements it in infrastructure.
 */
public interface EarthquakeEventPublisherPort {
    void publishDetected(EarthquakeDetectedEvent event);
}
