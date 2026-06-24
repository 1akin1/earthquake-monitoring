package com.afet.monitoring.infrastructure.feed.kandilli;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.GeoLocation;
import com.afet.monitoring.domain.model.Magnitude;
import com.afet.monitoring.domain.port.SeismicFeedPort;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The richest adapter: turns Kandilli's fixed-width TEXT table into domain objects.
 * Responsibilities it hides from the rest of the system:
 *  - tokenising whitespace-separated columns and skipping the header
 *  - picking the best available magnitude (Mw &gt; ML &gt; MD; "-.-" = missing)
 *  - parsing "yyyy.MM.dd HH:mm:ss" in Turkey time and normalising to UTC
 *  - rejoining the trailing location words
 */
public class KandilliFeedAdapter implements SeismicFeedPort {

    private static final String SOURCE = "Kandilli";
    private static final ZoneId TURKEY = ZoneId.of("Europe/Istanbul");
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    private final KandilliRawClient client;

    public KandilliFeedAdapter(KandilliRawClient client) {
        this.client = client;
    }

    @Override
    public String sourceName() {
        return SOURCE;
    }

    @Override
    public List<Earthquake> fetchRecent() {
        List<Earthquake> result = new ArrayList<>();
        for (String line : client.fetch().lines().toList()) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("Tarih")) {
                continue;   // skip blanks and the header row
            }
            String[] col = trimmed.split("\\s+");
            if (col.length < 9) {
                continue;   // malformed line, ignore (a real system would log it)
            }
            Double magnitude = preferredMagnitude(col[7], col[6], col[5]); // Mw, ML, MD
            if (magnitude == null) {
                continue;   // no usable magnitude on any scale
            }
            var occurredAt = LocalDateTime.parse(col[0] + " " + col[1], TS)
                    .atZone(TURKEY)
                    .toInstant();
            double lat = Double.parseDouble(col[2]);
            double lon = Double.parseDouble(col[3]);
            double depthKm = Double.parseDouble(col[4]);
            String location = String.join(" ", List.of(col).subList(8, col.length));

            result.add(Earthquake.register(
                    new Magnitude(magnitude), depthKm,
                    new GeoLocation(lat, lon), SOURCE, occurredAt));
        }
        return result;
    }

    /** First non-missing value, preferring the more authoritative scale (Mw, then ML, then MD). */
    private Double preferredMagnitude(String... candidatesBestFirst) {
        for (String c : candidatesBestFirst) {
            if (c != null && !c.equals("-.-")) {
                try {
                    return Double.parseDouble(c);
                } catch (NumberFormatException ignored) {
                    // not a number — try the next scale
                }
            }
        }
        return null;
    }
}
