package com.afet.monitoring.infrastructure.feed.kandilli;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.Charset;

/**
 * The adaptee for Kandilli Observatory (KOERI). KOERI publishes a fixed-width PLAIN-TEXT
 * table embedded in an HTML page, encoded in ISO-8859-9 (Turkish). {@link #fetch()} does a
 * REAL HTTP GET, decodes with the right charset and returns the text inside the &lt;pre&gt;
 * block — exactly the format {@code KandilliFeedAdapter} already parses. Empty on error.
 */
@Component
public class KandilliRawClient {

    private static final Charset LATIN5 = Charset.forName("ISO-8859-9");

    private final RestClient http = RestClient.builder()
            .baseUrl("http://www.koeri.boun.edu.tr")
            .build();

    public String fetch() {
        try {
            byte[] bytes = http.get()
                    .uri("/scripts/lst0.asp")
                    .retrieve()
                    .body(byte[].class);
            if (bytes == null) return "";
            String html = new String(bytes, LATIN5);
            int start = html.indexOf("<pre>");
            int end = html.indexOf("</pre>");
            if (start >= 0 && end > start) {
                return html.substring(start + "<pre>".length(), end);
            }
            return html; // no <pre> found — let the adapter skip non-data lines
        } catch (Exception e) {
            return "";   // KOERI unreachable — degrade to empty
        }
    }
}
