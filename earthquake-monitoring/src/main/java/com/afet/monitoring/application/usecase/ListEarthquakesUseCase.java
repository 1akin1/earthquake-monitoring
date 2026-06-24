package com.afet.monitoring.application.usecase;

import com.afet.monitoring.application.annotation.UseCase;
import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.port.EarthquakeRepositoryPort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@UseCase
public class ListEarthquakesUseCase {

    private final EarthquakeRepositoryPort repository;

    public ListEarthquakesUseCase(EarthquakeRepositoryPort repository) {
        this.repository = repository;
    }

    @Cacheable("earthquakeList")   // whole list cached as one entry; evicted on any write
    @Transactional(readOnly = true)
    public List<Earthquake> handle() {
        return repository.findAll();
    }
}
