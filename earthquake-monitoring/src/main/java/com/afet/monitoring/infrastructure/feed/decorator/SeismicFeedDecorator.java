package com.afet.monitoring.infrastructure.feed.decorator;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.port.SeismicFeedPort;

import java.util.List;

/**
 * Decorator Pattern — base class. A decorator <i>is</i> a {@link SeismicFeedPort} and
 * <i>wraps</i> one, so it can be stacked transparently in front of any feed (a raw
 * adapter or another decorator) to condition/enrich the raw event stream without the feed
 * or its callers knowing. The import/preview use cases keep depending only on
 * {@code SeismicFeedPort}; how many layers wrap a source is a wiring detail.
 *
 * <p>By default a decorator forwards everything to its delegate; concrete decorators
 * override {@link #fetchRecent()} to add one transformation around the delegate's output.
 * Pure logic — no framework; the stack is assembled in {@code FeedConfig}.
 */
public abstract class SeismicFeedDecorator implements SeismicFeedPort {

    protected final SeismicFeedPort delegate;

    protected SeismicFeedDecorator(SeismicFeedPort delegate) {
        this.delegate = delegate;
    }

    /** Decorating doesn't change the source identity. */
    @Override
    public String sourceName() {
        return delegate.sourceName();
    }

    @Override
    public List<Earthquake> fetchRecent() {
        return delegate.fetchRecent();
    }
}
