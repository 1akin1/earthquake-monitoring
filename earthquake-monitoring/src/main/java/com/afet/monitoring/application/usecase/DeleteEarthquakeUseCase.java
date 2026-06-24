package com.afet.monitoring.application.usecase;

import com.afet.monitoring.application.annotation.UseCase;
import com.afet.monitoring.domain.exception.EarthquakeNotFoundException;
import com.afet.monitoring.domain.port.EarthquakeRepositoryPort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class DeleteEarthquakeUseCase {

    private final EarthquakeRepositoryPort repository;

    public DeleteEarthquakeUseCase(EarthquakeRepositoryPort repository) {
        this.repository = repository;
    }

    @Caching(evict = {
            @CacheEvict(value = "earthquakes", key = "#id"),       // drop the single entry
            @CacheEvict(value = "earthquakeList", allEntries = true) // and the list
    })
    @Transactional
    public void handle(Long id) {
        if (repository.findById(id).isEmpty()) {
            throw new EarthquakeNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
