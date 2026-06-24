package com.afet.monitoring.infrastructure.config;

import com.afet.monitoring.domain.service.detection.EarthquakeDetector;
import com.afet.monitoring.domain.service.detection.StaLtaDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the detection algorithm as a bean. The domain detector stays
 * annotation-free; the tuning (window sizes, trigger ratio) lives here. Swapping the
 * algorithm later = change this one bean.
 */
@Configuration
public class DetectionConfig {

    @Bean
    EarthquakeDetector earthquakeDetector() {
        // STA = 10 samples, LTA = 100 samples, fire when short-term energy is 2.8x background.
        return new StaLtaDetector(10, 100, 2.8);
    }
}
