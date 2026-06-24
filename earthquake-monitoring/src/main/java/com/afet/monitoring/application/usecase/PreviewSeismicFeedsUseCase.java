package com.afet.monitoring.application.usecase;

import com.afet.monitoring.application.annotation.UseCase;
import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.port.SeismicFeedPort;
import java.util.List;

/**
 * Pulls recent events from EVERY seismic source and returns them normalised to the
 * domain model — without scoring or persisting. Pure read: shows the Adapter Pattern's
 * payoff (three incompatible APIs, one uniform result).
 *
 * <p>Spring injects every {@link SeismicFeedPort} bean into the list, so this class
 * never names Kandilli/AFAD/USGS — adding a fourth source changes nothing here.
 * No {@code @Transactional}: nothing is written.
 */
@UseCase
public class PreviewSeismicFeedsUseCase {

    private final List<SeismicFeedPort> feeds;

    public PreviewSeismicFeedsUseCase(List<SeismicFeedPort> feeds) {
        this.feeds = feeds;
    }

    public List<Earthquake> handle() {
        return feeds.stream()
                .flatMap(feed -> feed.fetchRecent().stream())
                .toList();
    }
}
