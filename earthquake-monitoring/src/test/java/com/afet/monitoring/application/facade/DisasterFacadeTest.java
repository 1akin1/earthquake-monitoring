package com.afet.monitoring.application.facade;

import com.afet.monitoring.application.usecase.AssessDisasterUseCase;
import com.afet.monitoring.application.usecase.DetectEarthquakeUseCase;
import com.afet.monitoring.application.usecase.GenerateReportUseCase;
import com.afet.monitoring.application.usecase.ImportResult;
import com.afet.monitoring.application.usecase.ImportSeismicFeedsUseCase;
import com.afet.monitoring.domain.model.SeismicReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Facade Pattern — verifies the facade coordinates the right use cases in the right order
 * and bundles their results, without adding logic of its own.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DisasterFacade coordinates the use cases behind one entry point")
class DisasterFacadeTest {

    @Mock ImportSeismicFeedsUseCase importFeeds;
    @Mock GenerateReportUseCase generateReport;
    @Mock DetectEarthquakeUseCase detectEarthquake;
    @Mock AssessDisasterUseCase assessDisaster;

    @Test
    @DisplayName("runMonitoringCycle imports first, then reports, and bundles both results")
    void cycle_imports_then_reports() {
        DisasterFacade facade =
                new DisasterFacade(importFeeds, generateReport, detectEarthquake, assessDisaster);

        ImportResult imported = new ImportResult(3, Map.of("USGS", 3));
        SeismicReport report = SeismicReport.builder().title("X").build();
        when(importFeeds.handle()).thenReturn(imported);
        when(generateReport.handle()).thenReturn(report);

        MonitoringCycleResult result = facade.runMonitoringCycle();

        assertThat(result.imported()).isSameAs(imported);
        assertThat(result.report()).isSameAs(report);

        // import must run before the report so the summary includes what was just imported
        InOrder order = inOrder(importFeeds, generateReport);
        order.verify(importFeeds).handle();
        order.verify(generateReport).handle();

        // the facade did not touch the unrelated use cases
        verifyNoInteractions(detectEarthquake, assessDisaster);
    }
}
