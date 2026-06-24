package com.afet.monitoring.infrastructure.feed.usgs;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.GeoLocation;
import com.afet.monitoring.domain.model.Magnitude;
import com.afet.monitoring.domain.port.SeismicFeedPort;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Adapter Pattern (object adapter, via composition): wraps {@link UsgsRawClient}
 * (the adaptee) and adapts its GeoJSON shape to our {@link SeismicFeedPort} target.
 * All the USGS-specific weirdness (lon-first coordinates, epoch-millis time) is
 * absorbed here so nothing downstream ever sees it.
 */
public class UsgsFeedAdapter implements SeismicFeedPort {

    private static final String SOURCE = "USGS";

    private final UsgsRawClient client;

    public UsgsFeedAdapter(UsgsRawClient client) {
        this.client = client;
    }

    @Override
    public String sourceName() {
        return SOURCE;
    }

    @Override
    public List<Earthquake> fetchRecent() {
        return client.fetch().features().stream()
                .filter(f -> f.properties() != null && f.properties().mag() != null)
                .map(this::toDomain)
                .toList();
    }

    private Earthquake toDomain(UsgsGeoJsonResponse.Feature f) {
        List<Double> coords = f.geometry().coordinates();
        double longitude = coords.get(0);   // GeoJSON puts longitude FIRST
        double latitude  = coords.get(1);
        double depthKm   = coords.get(2);
        Instant occurredAt = Instant.ofEpochMilli(f.properties().time());  // epoch ms is UTC already
        return Earthquake.register(
                new Magnitude(f.properties().mag()),
                depthKm,
                new GeoLocation(latitude, longitude),
                SOURCE,
                Objects.requireNonNull(occurredAt));
    }
}
