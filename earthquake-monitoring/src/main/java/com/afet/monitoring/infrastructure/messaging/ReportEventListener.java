package com.afet.monitoring.infrastructure.messaging;

import org.springframework.context.annotation.Profile;
import com.afet.monitoring.domain.event.EarthquakeDetectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Observer #2 — the reporting reaction. Updates running statistics for every event.
 * Distinct {@code groupId} from the alert consumer, so both see the same events.
 * (Stands in for a future report-service microservice.)
 */
@Profile("!platform") // consumer-side reaction; in the platform it lives in its own service
@Component
public class ReportEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReportEventListener.class);

    private final ReportStatistics statistics;

    public ReportEventListener(ReportStatistics statistics) {
        this.statistics = statistics;
    }

    @KafkaListener(topics = KafkaTopics.EARTHQUAKE_DETECTED, groupId = "report-service")
    public void onEarthquakeDetected(EarthquakeDetectedEvent event) {
        statistics.record(event.magnitude(), event.riskLevel());
        log.info("[REPORT] recorded M{} ({}). Totals now: {}",
                event.magnitude(), event.riskLevel(), statistics.total());
    }
}
