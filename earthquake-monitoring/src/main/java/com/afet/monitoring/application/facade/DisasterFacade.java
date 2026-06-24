package com.afet.monitoring.application.facade;

import com.afet.monitoring.application.usecase.AssessDisasterCommand;
import com.afet.monitoring.application.usecase.AssessDisasterUseCase;
import com.afet.monitoring.application.usecase.DetectEarthquakeCommand;
import com.afet.monitoring.application.usecase.DetectEarthquakeUseCase;
import com.afet.monitoring.application.usecase.DetectionOutcome;
import com.afet.monitoring.application.usecase.GenerateReportUseCase;
import com.afet.monitoring.application.usecase.ImportResult;
import com.afet.monitoring.application.usecase.ImportSeismicFeedsUseCase;
import com.afet.monitoring.domain.model.DisasterAssessment;
import com.afet.monitoring.domain.model.SeismicReport;
import org.springframework.stereotype.Service;

/**
 * Facade Pattern. The monitoring workflow is spread across several use cases (import,
 * report, detect, assess), each with its own dependencies and transaction rules. This
 * facade gives callers ONE simple object that hides that surface — most usefully via
 * {@link #runMonitoringCycle()}, which puts the whole "pull every feed, then summarise"
 * flow behind a single method.
 *
 * <p>It adds no business logic of its own; it only coordinates the use cases. They remain
 * independently usable and testable — the facade is a convenience layer, not a gatekeeper
 * (which is what keeps a facade from degrading into a god object).
 */
@Service
public class DisasterFacade {

    private final ImportSeismicFeedsUseCase importFeeds;
    private final GenerateReportUseCase generateReport;
    private final DetectEarthquakeUseCase detectEarthquake;
    private final AssessDisasterUseCase assessDisaster;

    public DisasterFacade(ImportSeismicFeedsUseCase importFeeds,
                          GenerateReportUseCase generateReport,
                          DetectEarthquakeUseCase detectEarthquake,
                          AssessDisasterUseCase assessDisaster) {
        this.importFeeds = importFeeds;
        this.generateReport = generateReport;
        this.detectEarthquake = detectEarthquake;
        this.assessDisaster = assessDisaster;
    }

    /**
     * The whole monitoring cycle behind one call: import every feed (normalise → score →
     * persist → publish), then take a fresh summary report over everything stored.
     */
    public MonitoringCycleResult runMonitoringCycle() {
        ImportResult imported = importFeeds.handle();
        SeismicReport report = generateReport.handle();
        return new MonitoringCycleResult(imported, report);
    }

    /** Analyse a raw signal window (detect → score → persist → publish). */
    public DetectionOutcome analyzeSignal(DetectEarthquakeCommand command) {
        return detectEarthquake.handle(command);
    }

    /** Assess a disaster reading for a given type and intensity. */
    public DisasterAssessment assess(AssessDisasterCommand command) {
        return assessDisaster.handle(command);
    }

    /** Current summary report over all stored earthquakes. */
    public SeismicReport currentReport() {
        return generateReport.handle();
    }
}
