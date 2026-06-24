package com.afet.monitoring.infrastructure.feed.decorator;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.port.SeismicFeedPort;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decorator — collapses duplicate events from a single fetch. Feeds occasionally repeat
 * the same event (a revised entry, an overlapping time window); two events are treated as
 * the same when they share a time, magnitude and location (rounded to ~100 m). The first
 * occurrence wins; later copies are dropped, preserving order.
 */
public class DeduplicatingFeedDecorator extends SeismicFeedDecorator {

    public DeduplicatingFeedDecorator(SeismicFeedPort delegate) {
        super(delegate);
    }

    @Override
    public List<Earthquake> fetchRecent() {
        Set<String> seen = new HashSet<>();
        List<Earthquake> unique = new ArrayList<>();
        for (Earthquake e : delegate.fetchRecent()) {
            if (seen.add(key(e))) {
                unique.add(e);
            }
        }
        return List.copyOf(unique);
    }

    private static String key(Earthquake e) {
        return e.occurredAt()
                + "|" + String.format(java.util.Locale.US, "%.1f", e.magnitude().value())
                + "|" + String.format(java.util.Locale.US, "%.3f", e.location().latitude())
                + "|" + String.format(java.util.Locale.US, "%.3f", e.location().longitude());
    }
}
