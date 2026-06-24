package com.afet.report.stats;

import com.afet.platform.events.RiskLevel;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAccumulator;

/** Thread-safe in-memory stats. In a real deployment this is the report-service's own DB. */
@Component
public class ReportStatistics {
    private final AtomicLong total = new AtomicLong();
    private final DoubleAccumulator maxMagnitude = new DoubleAccumulator(Math::max, 0.0);
    private final Map<RiskLevel, AtomicLong> byRiskLevel = new EnumMap<>(RiskLevel.class);

    public ReportStatistics() {
        for (RiskLevel level : RiskLevel.values()) byRiskLevel.put(level, new AtomicLong());
    }
    public void record(double magnitude, RiskLevel level) {
        total.incrementAndGet();
        maxMagnitude.accumulate(magnitude);
        if (level != null) byRiskLevel.get(level).incrementAndGet();
    }
    public long total() { return total.get(); }
    public double maxMagnitude() { return maxMagnitude.get(); }
    public Map<String, Long> byRiskLevel() {
        Map<String, Long> result = new LinkedHashMap<>();
        byRiskLevel.forEach((level, count) -> result.put(level.name(), count.get()));
        return result;
    }
}
