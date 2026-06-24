package com.afet.monitoring.presentation.controller;

import com.afet.monitoring.application.usecase.GenerateReportUseCase;
import com.afet.monitoring.domain.model.SeismicReport;
import com.afet.monitoring.domain.service.report.ReportFormat;
import com.afet.monitoring.domain.service.report.ReportRenderer;
import com.afet.monitoring.domain.service.report.ReportRendererFactory;
import com.afet.monitoring.presentation.controller.dto.ReportResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Thin adapter over the report use case. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final GenerateReportUseCase generateReport;
    private final ReportRendererFactory rendererFactory;

    public ReportController(GenerateReportUseCase generateReport,
                            ReportRendererFactory rendererFactory) {
        this.generateReport = generateReport;
        this.rendererFactory = rendererFactory;
    }

    /** Build and return a summary report over all stored earthquakes (JSON). */
    @GetMapping
    public ReportResponse report() {
        return ReportResponse.from(generateReport.handle());
    }

    /**
     * Render the same report into a chosen text format (Template Method). The renderer
     * owns the layout; this endpoint just selects one by {@code ?format=text|markdown}
     * and forwards the matching content type. Unknown formats fall back to plain text.
     */
    @GetMapping("/render")
    public ResponseEntity<String> render(@RequestParam(defaultValue = "text") String format) {
        SeismicReport report = generateReport.handle();
        ReportRenderer renderer = rendererFactory.forFormat(ReportFormat.fromString(format));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(renderer.contentType() + ";charset=UTF-8"))
                .body(renderer.render(report));
    }
}
