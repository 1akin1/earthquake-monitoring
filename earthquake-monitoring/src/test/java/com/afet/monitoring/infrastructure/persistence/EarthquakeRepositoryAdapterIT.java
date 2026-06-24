package com.afet.monitoring.infrastructure.persistence;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.GeoLocation;
import com.afet.monitoring.domain.model.Magnitude;
import com.afet.monitoring.domain.model.RiskLevel;
import com.afet.monitoring.domain.model.RiskScore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the persistence adapter against a REAL PostgreSQL running in a
 * throwaway Docker container (Testcontainers).
 *
 * <p>This exercises the full persistence seam end to end:
 * <ol>
 *   <li>Flyway applies {@code V1__create_earthquakes.sql} to the fresh container,</li>
 *   <li>Hibernate then runs with {@code ddl-auto=validate} — so the test also proves
 *       the migration and the {@code @Entity} mapping actually agree,</li>
 *   <li>the adapter maps domain {@code Earthquake} &lt;-&gt; {@code JpaEarthquakeEntity}
 *       both ways, including the nullable risk score.</li>
 * </ol>
 *
 * <p>Only the JPA slice is loaded ({@code @DataJpaTest}); Redis and Kafka are NOT
 * started, so no extra infrastructure is needed beyond Docker.
 *
 * <p>Run with: {@code mvn verify} (Failsafe runs *IT classes; {@code mvn test} skips them).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // use the container, not embedded H2
@Import(EarthquakeRepositoryAdapter.class)  // the adapter is a @Component — pull it into the slice
@Testcontainers
@DisplayName("EarthquakeRepositoryAdapter against real PostgreSQL")
class EarthquakeRepositoryAdapterIT {

    @Container
    @ServiceConnection  // Spring Boot wires datasource url/user/password from the container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    EarthquakeRepositoryAdapter adapter;

    /** Fixed, microsecond-clean timestamp so equality holds across the DB round-trip. */
    private static final Instant OCCURRED_AT = Instant.parse("2026-06-20T08:30:00Z");

    private static Earthquake newQuake() {
        return Earthquake.register(
                new Magnitude(6.4), 12.5,
                new GeoLocation(40.65, 29.27), "AFAD", OCCURRED_AT);
    }

    @Test
    @DisplayName("save assigns an id and every field round-trips through findById")
    void save_and_find() {
        Earthquake saved = adapter.save(newQuake());

        assertThat(saved.id()).isNotNull();

        Optional<Earthquake> found = adapter.findById(saved.id());
        assertThat(found).isPresent();

        Earthquake e = found.get();
        assertThat(e.magnitude().value()).isEqualTo(6.4);
        assertThat(e.depthKm()).isEqualTo(12.5);
        assertThat(e.location()).isEqualTo(new GeoLocation(40.65, 29.27));
        assertThat(e.source()).isEqualTo("AFAD");
        assertThat(e.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(e.riskScore()).isNull(); // not scored yet
    }

    @Test
    @DisplayName("risk score and level survive the round-trip (enum stored as string)")
    void persists_risk_score() {
        Earthquake scored = newQuake().assessedWith(new RiskScore(72.5, RiskLevel.HIGH));

        Earthquake saved = adapter.save(scored);
        Earthquake reloaded = adapter.findById(saved.id()).orElseThrow();

        assertThat(reloaded.riskScore()).isNotNull();
        assertThat(reloaded.riskScore().value()).isEqualTo(72.5);
        assertThat(reloaded.riskScore().level()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    @DisplayName("findAll returns every saved earthquake")
    void find_all() {
        adapter.save(newQuake());
        adapter.save(newQuake());

        assertThat(adapter.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("deleteById removes the row")
    void delete_by_id() {
        Earthquake saved = adapter.save(newQuake());

        adapter.deleteById(saved.id());

        assertThat(adapter.findById(saved.id())).isEmpty();
    }
}
