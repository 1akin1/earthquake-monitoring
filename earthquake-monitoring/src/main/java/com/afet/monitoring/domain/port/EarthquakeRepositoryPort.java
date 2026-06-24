package com.afet.monitoring.domain.port;

import com.afet.monitoring.domain.model.Earthquake;
import java.util.List;
import java.util.Optional;

/**
 * Port (DIP boundary). The domain/application depends on THIS interface,
 * never on JPA. The infrastructure layer provides an adapter that implements it.
 */
public interface EarthquakeRepositoryPort {
    Earthquake save(Earthquake earthquake);
    Optional<Earthquake> findById(Long id);
    List<Earthquake> findAll();
    void deleteById(Long id);
}
