package com.afet.monitoring.application.usecase;

import com.afet.monitoring.application.annotation.UseCase;
import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.SeismicReport;
import com.afet.monitoring.domain.port.EarthquakeRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Builds a {@link SeismicReport} summarising every earthquake currently stored. The
 * aggregation lives in the report's Builder; this use case just fetches the data and
 * orchestrates assembly.
 */
@UseCase
public class GenerateReportUseCase {

    private final EarthquakeRepositoryPort repository;

    public GenerateReportUseCase(EarthquakeRepositoryPort repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SeismicReport handle() {
        List<Earthquake> earthquakes = repository.findAll();
        return SeismicReport.builder()
                .title("Earthquake summary — all records")
                .generatedAt(Instant.now())
                .addAll(earthquakes)
                .build();
    }
}
