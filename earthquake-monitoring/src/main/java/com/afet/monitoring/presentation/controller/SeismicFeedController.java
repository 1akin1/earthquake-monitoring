package com.afet.monitoring.presentation.controller;

import com.afet.monitoring.application.usecase.ImportSeismicFeedsUseCase;
import com.afet.monitoring.application.usecase.PreviewSeismicFeedsUseCase;
import com.afet.monitoring.presentation.controller.dto.EarthquakeResponse;
import com.afet.monitoring.presentation.controller.dto.ImportResultResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Thin adapter over the feed use cases. */
@RestController
@RequestMapping("/api/feeds")
public class SeismicFeedController {

    private final PreviewSeismicFeedsUseCase previewFeeds;
    private final ImportSeismicFeedsUseCase importFeeds;

    public SeismicFeedController(PreviewSeismicFeedsUseCase previewFeeds,
                                 ImportSeismicFeedsUseCase importFeeds) {
        this.previewFeeds = previewFeeds;
        this.importFeeds = importFeeds;
    }

    /** Normalised events from all sources, NOT persisted. Pure Adapter demo (no DB write). */
    @GetMapping("/preview")
    public List<EarthquakeResponse> preview() {
        return previewFeeds.handle().stream().map(EarthquakeResponse::from).toList();
    }

    /** Pull from all sources, score each, persist. Returns a per-source summary. */
    @PostMapping("/import")
    public ImportResultResponse importAll() {
        return ImportResultResponse.from(importFeeds.handle());
    }
}
