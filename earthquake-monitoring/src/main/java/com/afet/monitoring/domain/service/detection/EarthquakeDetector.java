package com.afet.monitoring.domain.service.detection;

import com.afet.monitoring.domain.model.DetectionResult;
import com.afet.monitoring.domain.model.SeismicSignal;

/**
 * Detection algorithm abstraction. Different algorithms (STA/LTA, template matching,
 * ML) can implement this — the use case depends only on the interface, so the
 * detection method is swappable (Strategy-style, DIP). Pure domain.
 */
public interface EarthquakeDetector {
    DetectionResult analyze(SeismicSignal signal);
}
