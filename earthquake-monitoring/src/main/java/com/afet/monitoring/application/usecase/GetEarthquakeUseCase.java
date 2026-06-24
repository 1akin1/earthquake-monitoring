package com.afet.monitoring.application.usecase;

import com.afet.monitoring.application.annotation.UseCase;
import com.afet.monitoring.domain.exception.EarthquakeNotFoundException;
import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.port.EarthquakeRepositoryPort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class GetEarthquakeUseCase {

    private final EarthquakeRepositoryPort repository;

    public GetEarthquakeUseCase(EarthquakeRepositoryPort repository) {
        this.repository = repository;
    }

    @Cacheable(value = "earthquakes", key = "#id")   // read-through: hit Redis before the DB
    @Transactional(readOnly = true)
    public Earthquake handle(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EarthquakeNotFoundException(id));
    }
}
