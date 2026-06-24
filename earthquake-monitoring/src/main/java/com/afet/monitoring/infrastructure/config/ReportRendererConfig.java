package com.afet.monitoring.infrastructure.config;

import com.afet.monitoring.domain.service.report.MarkdownReportRenderer;
import com.afet.monitoring.domain.service.report.PlainTextReportRenderer;
import com.afet.monitoring.domain.service.report.ReportRenderer;
import com.afet.monitoring.domain.service.report.ReportRendererFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * The ONLY Spring-aware place for the report renderers. The renderers themselves stay
 * pure domain (Template Method); here they become beans and Spring injects every
 * {@link ReportRenderer} into the factory's list — assembled without naming each one.
 *
 * <p>Add a new format later: write the renderer, add one {@code @Bean} line. Neither the
 * factory nor any existing renderer is touched (OCP). Same shape as
 * {@code DisasterFactoryConfig}.
 */
@Configuration
public class ReportRendererConfig {

    @Bean ReportRenderer plainTextReportRenderer() { return new PlainTextReportRenderer(); }
    @Bean ReportRenderer markdownReportRenderer()  { return new MarkdownReportRenderer(); }

    @Bean
    ReportRendererFactory reportRendererFactory(List<ReportRenderer> renderers) {
        return new ReportRendererFactory(renderers);
    }
}
