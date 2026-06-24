package com.afet.monitoring.infrastructure.persistence;

import com.afet.monitoring.domain.model.RiskLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "earthquakes", indexes = {
        @Index(name = "idx_eq_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_eq_lat_lon", columnList = "latitude, longitude"),
        @Index(name = "idx_eq_risk_level", columnList = "risk_level")
})
@Getter
@Setter
@NoArgsConstructor
public class JpaEarthquakeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private double magnitude;

    @Column(name = "depth_km", nullable = false)
    private double depthKm;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private String source;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "risk_score")
    private Double riskScore;             // nullable

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;          // nullable

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
