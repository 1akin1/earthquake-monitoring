package com.afet.report.consumer;

import com.afet.platform.events.EarthquakeDetectedEvent;
import com.afet.platform.events.Topics;
import com.afet.report.stats.ReportStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** The reporting reaction, now its own service. groupId "report-service" (unchanged). */
@Component
public class ReportEventListener {
    private static final Logger log = LoggerFactory.getLogger(ReportEventListener.class);
    private final ReportStatistics statistics;

    public ReportEventListener(ReportStatistics statistics) { this.statistics = statistics; }

    @KafkaListener(topics = Topics.EARTHQUAKE_DETECTED, groupId = "report-service")
    public void onEarthquakeDetected(EarthquakeDetectedEvent event) {
        statistics.record(event.magnitude(), event.riskLevel());
        log.info("[REPORT] recorded M{} ({}). Totals now: {}",
                event.magnitude(), event.riskLevel(), statistics.total());
    }
}
