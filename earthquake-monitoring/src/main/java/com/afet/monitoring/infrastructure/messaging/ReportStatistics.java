package com.afet.monitoring.infrastructure.messaging;

import org.springframework.context.annotation.Profile;
import com.afet.monitoring.domain.model.RiskLevel;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAccumulator;

/**
 * In-memory statistics maintained by the report consumer. Thread-safe because Kafka
 * listener containers may invoke from multiple threads. In a real system this would be
 * the report-service's own database; here it makes the Observer fan-out observable.
 */
@Profile("!platform") // consumer-side reaction; in the platform it lives in its own service
@Component
public class ReportStatistics {

    private final AtomicLong total = new AtomicLong();
    private final DoubleAccumulator maxMagnitude = new DoubleAccumulator(Math::max, 0.0);
    private final Map<RiskLevel, AtomicLong> byRiskLevel = new EnumMap<>(RiskLevel.class);

    public ReportStatistics() {
        for (RiskLevel level : RiskLevel.values()) {
            byRiskLevel.put(level, new AtomicLong());
        }
    }

    public void record(double magnitude, RiskLevel level) {
        total.incrementAndGet();
        maxMagnitude.accumulate(magnitude);
        if (level != null) {
            byRiskLevel.get(level).incrementAndGet();
        }
    }

    public long total() { return total.get(); }
    public double maxMagnitude() { return maxMagnitude.get(); }

    public Map<String, Long> byRiskLevel() {
        Map<String, Long> result = new java.util.LinkedHashMap<>();
        byRiskLevel.forEach((level, count) -> result.put(level.name(), count.get()));
        return result;
    }
}
