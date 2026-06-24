package com.afet.monitoring.application.usecase;

import com.afet.monitoring.application.annotation.UseCase;
import com.afet.monitoring.domain.event.EarthquakeDetectedEvent;
import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.RiskScore;
import com.afet.monitoring.domain.port.EarthquakeEventPublisherPort;
import com.afet.monitoring.domain.port.EarthquakeRepositoryPort;
import com.afet.monitoring.domain.port.SeismicFeedPort;
import com.afet.monitoring.domain.service.RiskScoringService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The pattern payoff in one flow: <b>Adapter</b> normalises every source -&gt;
 * <b>Strategy</b> scores each event -&gt; repository (another Adapter) persists it -&gt;
 * an event is published so the <b>Observer</b> consumers react.
 * Depends only on abstractions: the feed ports, the scoring service, the repo + event ports.
 */
@UseCase
public class ImportSeismicFeedsUseCase {

    private final List<SeismicFeedPort> feeds;
    private final RiskScoringService riskScoringService;
    private final EarthquakeRepositoryPort repository;
    private final EarthquakeEventPublisherPort eventPublisher;

    public ImportSeismicFeedsUseCase(List<SeismicFeedPort> feeds,
                                     RiskScoringService riskScoringService,
                                     EarthquakeRepositoryPort repository,
                                     EarthquakeEventPublisherPort eventPublisher) {
        this.feeds = feeds;
        this.riskScoringService = riskScoringService;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @CacheEvict(value = "earthquakeList", allEntries = true)  // bulk insert invalidates the list
    @Transactional
    public ImportResult handle() {
        // Cross-call de-duplication: feeds return the same recent events every time, so
        // without this an import re-saves them and the database grows on every click.
        // We seed a set with the natural keys already stored, then skip any incoming
        // event whose key is already present (in the DB or earlier in this same run).
        Set<String> known = new HashSet<>();
        for (Earthquake existing : repository.findAll()) {
            known.add(naturalKey(existing));
        }

        Map<String, Integer> bySource = new LinkedHashMap<>();
        int total = 0;
        for (SeismicFeedPort feed : feeds) {
            int count = 0;
            for (Earthquake normalised : feed.fetchRecent()) {     // Adapter output
                if (!known.add(naturalKey(normalised))) {
                    continue; // already imported in a previous run (or duplicate this run)
                }
                RiskScore risk = riskScoringService.assess(normalised); // Strategy
                Earthquake saved = repository.save(normalised.assessedWith(risk)); // persist
                eventPublisher.publishDetected(EarthquakeDetectedEvent.from(saved)); // Observer
                count++;
            }
            bySource.put(feed.sourceName(), count);
            total += count;
        }
        return new ImportResult(total, bySource);
    }

    /**
     * Natural identity of an event: same time, magnitude and location (~100 m) means the
     * same earthquake. Matches the in-fetch DeduplicatingFeedDecorator's key so the two
     * dedup layers agree.
     */
    private static String naturalKey(Earthquake e) {
        return e.occurredAt()
                + "|" + String.format(Locale.US, "%.1f", e.magnitude().value())
                + "|" + String.format(Locale.US, "%.3f", e.location().latitude())
                + "|" + String.format(Locale.US, "%.3f", e.location().longitude());
    }
}
