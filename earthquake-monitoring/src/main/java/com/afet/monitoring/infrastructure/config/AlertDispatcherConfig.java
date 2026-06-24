package com.afet.monitoring.infrastructure.config;

import org.springframework.context.annotation.Profile;
import com.afet.monitoring.domain.service.alert.AlertDispatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Command invoker. The dispatcher stays pure domain; its one tuning knob — how
 * many times to retry a flaky alert channel — is set here.
 */
@Profile("!platform") // consumer-side reaction; in the platform it lives in its own service
@Configuration
public class AlertDispatcherConfig {

    /** Try an alert up to three times before giving up and rolling the batch back. */
    private static final int MAX_ATTEMPTS = 3;

    @Bean
    AlertDispatcher alertDispatcher() {
        return new AlertDispatcher(MAX_ATTEMPTS);
    }
}
