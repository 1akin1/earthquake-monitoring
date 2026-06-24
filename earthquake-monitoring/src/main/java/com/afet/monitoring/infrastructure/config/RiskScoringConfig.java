package com.afet.monitoring.infrastructure.config;

import com.afet.monitoring.domain.service.RiskScoringService;
import com.afet.monitoring.domain.service.RiskScoringStrategy;
import com.afet.monitoring.domain.service.strategy.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

/**
 * The ONLY place that knows both Spring and the concrete strategies. Domain strategies
 * stay annotation-free; here we register them as beans and assemble the domain service.
 * Spring injects every RiskScoringStrategy bean into the List below.
 */
@Configuration
public class RiskScoringConfig {

    @Bean RiskScoringStrategy lowRiskStrategy()      { return new LowRiskStrategy(); }
    @Bean RiskScoringStrategy mediumRiskStrategy()   { return new MediumRiskStrategy(); }
    @Bean RiskScoringStrategy highRiskStrategy()     { return new HighRiskStrategy(); }
    @Bean RiskScoringStrategy criticalRiskStrategy() { return new CriticalRiskStrategy(); }

    @Bean
    RiskScoringService riskScoringService(List<RiskScoringStrategy> strategies) {
        return new RiskScoringService(strategies);
    }
}
