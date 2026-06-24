package com.afet.monitoring.infrastructure.feed.decorator;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.port.SeismicFeedPort;

import java.util.List;

/**
 * Decorator — drops events below a magnitude floor. Public feeds are full of tiny
 * micro-quakes that are noise for an alerting platform; filtering them at the edge keeps
 * the rest of the pipeline focused on events that matter.
 */
public class MinMagnitudeFeedDecorator extends SeismicFeedDecorator {

    private final double floor;

    public MinMagnitudeFeedDecorator(SeismicFeedPort delegate, double floor) {
        super(delegate);
        this.floor = floor;
    }

    @Override
    public List<Earthquake> fetchRecent() {
        return delegate.fetchRecent().stream()
                .filter(e -> e.magnitude().value() >= floor)
                .toList();
    }
}
