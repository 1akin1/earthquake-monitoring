package com.afet.monitoring.infrastructure.feed.afad;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The adaptee for AFAD. A REAL HTTP call to AFAD's public apiv2 event filter, pulling the
 * last 30 days of events in Turkey. Returns an empty list on any error so the app keeps
 * running offline. Official endpoint: https://deprem.afad.gov.tr/apiv2/event/filter
 */
@Component
public class AfadRawClient {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final RestClient http = RestClient.builder()
            .baseUrl("https://deprem.afad.gov.tr")
            .build();

    public List<AfadEventDto> fetch() {
        String end = LocalDateTime.now().format(FMT);
        String start = LocalDateTime.now().minusDays(30).format(FMT);
        try {
            List<AfadEventDto> res = http.get()
                    .uri(uri -> uri.path("/apiv2/event/filter")
                            .queryParam("start", start)
                            .queryParam("end", end)
                            .queryParam("orderby", "timedesc")
                            .queryParam("limit", 5000)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<AfadEventDto>>() {});
            return res != null ? res : List.of();
        } catch (Exception e) {
            return List.of();   // AFAD unreachable — degrade to empty
        }
    }
}
