package com.afet.monitoring.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository — an infrastructure detail, hidden behind the adapter. */
public interface EarthquakeJpaRepository extends JpaRepository<JpaEarthquakeEntity, Long> {
}
