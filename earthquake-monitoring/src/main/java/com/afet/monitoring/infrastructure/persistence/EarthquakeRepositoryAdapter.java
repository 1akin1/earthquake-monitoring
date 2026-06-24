package com.afet.monitoring.infrastructure.persistence;

import com.afet.monitoring.domain.model.*;
import com.afet.monitoring.domain.port.EarthquakeRepositoryPort;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

/**
 * Adapter Pattern + heart of Hexagonal: implements the domain PORT with JPA and maps
 * the pure domain model <-> persistence entity both ways, including the risk score.
 */
@Component
public class EarthquakeRepositoryAdapter implements EarthquakeRepositoryPort {

    private final EarthquakeJpaRepository jpaRepository;

    public EarthquakeRepositoryAdapter(EarthquakeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Earthquake save(Earthquake earthquake) {
        return toDomain(jpaRepository.save(toEntity(earthquake)));
    }

    @Override
    public Optional<Earthquake> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Earthquake> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    // ── Mapping: domain <-> persistence ──────────────────────────────────────
    private JpaEarthquakeEntity toEntity(Earthquake e) {
        JpaEarthquakeEntity entity = new JpaEarthquakeEntity();
        entity.setId(e.id());
        entity.setMagnitude(e.magnitude().value());
        entity.setDepthKm(e.depthKm());
        entity.setLatitude(e.location().latitude());
        entity.setLongitude(e.location().longitude());
        entity.setSource(e.source());
        entity.setOccurredAt(e.occurredAt());
        if (e.riskScore() != null) {
            entity.setRiskScore(e.riskScore().value());
            entity.setRiskLevel(e.riskScore().level());
        }
        return entity;
    }

    private Earthquake toDomain(JpaEarthquakeEntity entity) {
        RiskScore riskScore = (entity.getRiskScore() != null && entity.getRiskLevel() != null)
                ? new RiskScore(entity.getRiskScore(), entity.getRiskLevel())
                : null;
        return Earthquake.reconstitute(
                entity.getId(),
                new Magnitude(entity.getMagnitude()),
                entity.getDepthKm(),
                new GeoLocation(entity.getLatitude(), entity.getLongitude()),
                entity.getSource(),
                entity.getOccurredAt(),
                riskScore);
    }
}
