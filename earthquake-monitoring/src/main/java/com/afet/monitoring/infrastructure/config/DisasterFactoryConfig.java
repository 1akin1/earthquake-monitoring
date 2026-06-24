package com.afet.monitoring.infrastructure.config;

import com.afet.monitoring.domain.service.disaster.DisasterHandler;
import com.afet.monitoring.domain.service.disaster.DisasterHandlerFactory;
import com.afet.monitoring.domain.service.disaster.EarthquakeDisasterHandler;
import com.afet.monitoring.domain.service.disaster.FloodDisasterHandler;
import com.afet.monitoring.domain.service.disaster.WildfireDisasterHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

/**
 * The ONLY Spring-aware place for the disaster Factory. Handlers stay pure domain;
 * here they become beans, and Spring injects every {@link DisasterHandler} into the
 * list below — so the factory is assembled without naming each handler explicitly.
 *
 * <p>To add a new disaster type later: write the handler, add one {@code @Bean} line.
 * Neither the factory nor any existing handler is touched (OCP).
 */
@Configuration
public class DisasterFactoryConfig {

    @Bean DisasterHandler earthquakeDisasterHandler() { return new EarthquakeDisasterHandler(); }
    @Bean DisasterHandler floodDisasterHandler()      { return new FloodDisasterHandler(); }
    @Bean DisasterHandler wildfireDisasterHandler()   { return new WildfireDisasterHandler(); }

    @Bean
    DisasterHandlerFactory disasterHandlerFactory(List<DisasterHandler> handlers) {
        return new DisasterHandlerFactory(handlers);
    }
}
