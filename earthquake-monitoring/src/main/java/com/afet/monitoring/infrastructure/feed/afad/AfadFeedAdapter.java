package com.afet.monitoring.infrastructure.feed.afad;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.GeoLocation;
import com.afet.monitoring.domain.model.Magnitude;
import com.afet.monitoring.domain.port.SeismicFeedPort;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for AFAD. Wraps {@link AfadRawClient}, parses AFAD's string-typed fields, and
 * converts AFAD's zone-less local timestamp (Turkey time) into a proper UTC Instant so
 * every source ends up on the same clock. Bad/partial rows are skipped defensively.
 */
public class AfadFeedAdapter implements SeismicFeedPort {

    private static final String SOURCE = "AFAD";
    private static final ZoneId TURKEY = ZoneId.of("Europe/Istanbul");

    private final AfadRawClient client;

    public AfadFeedAdapter(AfadRawClient client) {
        this.client = client;
    }

    @Override
    public String sourceName() {
        return SOURCE;
    }

    @Override
    public List<Earthquake> fetchRecent() {
        List<Earthquake> result = new ArrayList<>();
        for (AfadEventDto dto : client.fetch()) {
            try {
                var occurredAt = LocalDateTime.parse(dto.date())   // ISO local, no zone
                        .atZone(TURKEY)                            // it's Turkey time...
                        .toInstant();                              // ...normalise to UTC
                result.add(Earthquake.register(
                        new Magnitude(Double.parseDouble(dto.magnitude())),
                        Double.parseDouble(dto.depth()),
                        new GeoLocation(Double.parseDouble(dto.latitude()), Double.parseDouble(dto.longitude())),
                        SOURCE,
                        occurredAt));
            } catch (Exception ignored) {
                // malformed row (missing magnitude/coords/date) — skip it
            }
        }
        return result;
    }
}
