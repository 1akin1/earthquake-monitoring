package com.afet.monitoring.infrastructure.feed.usgs;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

/**
 * The "adaptee": talks to USGS in its native GeoJSON format. This is now a REAL HTTP call
 * to the public USGS FDSN event service (no API key needed). It pulls the last year of
 * M4.5+ events worldwide, so the dashboard's day / week / month / year filters all have
 * genuine data to slice. On any network error it falls back to an empty feed so the
 * application still starts when offline.
 */
@Component
public class UsgsRawClient {

    private static final double MIN_MAGNITUDE = 4.5;   // keep volume sane; drop micro-quakes
    private static final int LIMIT = 1500;             // cap the response size

    private final RestClient http = RestClient.builder()
            .baseUrl("https://earthquake.usgs.gov")
            .build();

    public UsgsGeoJsonResponse fetch() {
        String starttime = LocalDate.now().minusYears(1).toString(); // yyyy-MM-dd
        try {
            UsgsGeoJsonResponse res = http.get()
                    .uri(uri -> uri.path("/fdsnws/event/1/query")
                            .queryParam("format", "geojson")
                            .queryParam("starttime", starttime)
                            .queryParam("minmagnitude", MIN_MAGNITUDE)
                            .queryParam("orderby", "time")
                            .queryParam("limit", LIMIT)
                            .build())
                    .retrieve()
                    .body(UsgsGeoJsonResponse.class);
            return res != null ? res : new UsgsGeoJsonResponse(List.of());
        } catch (Exception e) {
            // USGS unreachable (offline, rate-limited, etc.) — degrade to an empty feed.
            return new UsgsGeoJsonResponse(List.of());
        }
    }
}
