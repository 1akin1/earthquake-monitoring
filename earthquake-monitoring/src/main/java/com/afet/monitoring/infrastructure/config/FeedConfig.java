package com.afet.monitoring.infrastructure.config;

import com.afet.monitoring.domain.port.SeismicFeedPort;
import com.afet.monitoring.infrastructure.feed.afad.AfadFeedAdapter;
import com.afet.monitoring.infrastructure.feed.afad.AfadRawClient;
import com.afet.monitoring.infrastructure.feed.decorator.DeduplicatingFeedDecorator;
import com.afet.monitoring.infrastructure.feed.decorator.DepthDefaultingFeedDecorator;
import com.afet.monitoring.infrastructure.feed.decorator.MinMagnitudeFeedDecorator;
import com.afet.monitoring.infrastructure.feed.kandilli.KandilliFeedAdapter;
import com.afet.monitoring.infrastructure.feed.kandilli.KandilliRawClient;
import com.afet.monitoring.infrastructure.feed.usgs.UsgsFeedAdapter;
import com.afet.monitoring.infrastructure.feed.usgs.UsgsRawClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The ONLY Spring-aware place for the seismic feeds. Each raw adapter is constructed here
 * (its {@code RawClient} stays a {@code @Component}) and wrapped in the same Decorator
 * stack, so every source is conditioned identically before it reaches the pipeline:
 *
 * <pre>
 *   raw adapter → MinMagnitude (drop noise) → DepthDefaulting (enrich) → Deduplicating
 * </pre>
 *
 * <p>Order matters: filter cheap noise first, enrich what survives, then de-duplicate.
 * The import/preview use cases inject {@code List&lt;SeismicFeedPort&gt;} and receive
 * exactly these three decorated feeds — they never see the raw adapters or the layers.
 * Same construct-in-config idiom as the disaster handlers.
 */
@Configuration
public class FeedConfig {

    /** Ignore micro-quakes below this magnitude. */
    private static final double MIN_MAGNITUDE = 2.5;

    /** Substituted when a feed reports no usable depth. */
    private static final double DEFAULT_DEPTH_KM = 10.0;

    private SeismicFeedPort decorate(SeismicFeedPort raw) {
        return new DeduplicatingFeedDecorator(
                new DepthDefaultingFeedDecorator(
                        new MinMagnitudeFeedDecorator(raw, MIN_MAGNITUDE),
                        DEFAULT_DEPTH_KM));
    }

    @Bean
    SeismicFeedPort afadFeed(AfadRawClient client) {
        return decorate(new AfadFeedAdapter(client));
    }

    @Bean
    SeismicFeedPort kandilliFeed(KandilliRawClient client) {
        return decorate(new KandilliFeedAdapter(client));
    }

    @Bean
    SeismicFeedPort usgsFeed(UsgsRawClient client) {
        return decorate(new UsgsFeedAdapter(client));
    }
}
