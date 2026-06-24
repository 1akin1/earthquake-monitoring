package com.afet.alert.config;

import com.afet.alert.command.AlertDispatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlertDispatcherConfig {
    @Bean AlertDispatcher alertDispatcher() { return new AlertDispatcher(3); }
}
