package com.afet.monitoring.presentation.controller;

import com.afet.monitoring.application.usecase.DetectEarthquakeCommand;
import com.afet.monitoring.application.usecase.DetectEarthquakeUseCase;
import com.afet.monitoring.application.usecase.EvaluateSignalsUseCase;
import com.afet.monitoring.presentation.controller.dto.AnalyzeSignalRequest;
import com.afet.monitoring.presentation.controller.dto.DetectionResponse;
import com.afet.monitoring.presentation.controller.dto.EvaluateResponse;
import com.afet.monitoring.presentation.controller.dto.EvaluateSignalsRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Thin adapter over the detection use cases. */
@RestController
@RequestMapping("/api/detection")
public class DetectionController {

    private final DetectEarthquakeUseCase detectEarthquake;
    private final EvaluateSignalsUseCase evaluateSignals;

    public DetectionController(DetectEarthquakeUseCase detectEarthquake,
                               EvaluateSignalsUseCase evaluateSignals) {
        this.detectEarthquake = detectEarthquake;
        this.evaluateSignals = evaluateSignals;
    }

    /** Analyse a raw signal window; if an earthquake is detected, it is scored, saved and published. */
    @PostMapping("/analyze")
    public DetectionResponse analyze(@Valid @RequestBody AnalyzeSignalRequest request) {
        var command = new DetectEarthquakeCommand(
                request.stationId(), request.latitude(), request.longitude(),
                request.sampleRateHz(), request.startTime(), request.amplitudes());
        return DetectionResponse.from(detectEarthquake.handle(command));
    }

    /**
     * Evaluate a batch of signals read-only: runs the same validate -> detect -> score
     * pipeline as {@link #analyze}, but persists and publishes NOTHING. Backs the UI's
     * "evaluate all events" sweep so an operator can audit detector behaviour without
     * creating any synthetic earthquakes.
     */
    @PostMapping("/evaluate")
    public EvaluateResponse evaluate(@Valid @RequestBody EvaluateSignalsRequest request) {
        List<DetectEarthquakeCommand> commands = request.signals().stream()
                .map(s -> new DetectEarthquakeCommand(
                        s.stationId(), s.latitude(), s.longitude(),
                        s.sampleRateHz(), s.startTime(), s.amplitudes()))
                .toList();
        return EvaluateResponse.from(evaluateSignals.handle(commands));
    }
}
