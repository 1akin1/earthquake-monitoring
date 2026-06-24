package com.afet.monitoring.application.usecase;

import com.afet.monitoring.application.annotation.UseCase;
import com.afet.monitoring.domain.exception.SignalRejectedException;
import com.afet.monitoring.domain.model.DetectionResult;
import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.GeoLocation;
import com.afet.monitoring.domain.model.Magnitude;
import com.afet.monitoring.domain.model.RiskScore;
import com.afet.monitoring.domain.model.SeismicSignal;
import com.afet.monitoring.domain.service.RiskScoringService;
import com.afet.monitoring.domain.service.detection.EarthquakeDetector;
import com.afet.monitoring.domain.service.validation.SignalValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the REAL detection pipeline (validate &rarr; STA/LTA detect &rarr; risk score)
 * over a batch of signals WITHOUT persisting or publishing anything.
 *
 * <p>It deliberately depends on neither the {@code EarthquakeRepositoryPort} nor the
 * {@code EarthquakeEventPublisherPort}, so it is structurally impossible for this sweep
 * to write a row or emit an event — it is a pure read-only audit. (Contrast with
 * {@link DetectEarthquakeUseCase}, which persists and publishes on a hit.)
 *
 * <p>A signal the validation chain rejects is reported as a non-detection rather than
 * aborting the whole batch, so one unusable window never sinks the sweep.
 */
@UseCase
public class EvaluateSignalsUseCase {

    /** Single-station signals can't resolve depth; mirror the detection use case's default. */
    private static final double DEFAULT_DEPTH_KM = 10.0;

    private final EarthquakeDetector detector;
    private final SignalValidator signalValidationChain;
    private final RiskScoringService riskScoringService;

    public EvaluateSignalsUseCase(EarthquakeDetector detector,
                                  SignalValidator signalValidationChain,
                                  RiskScoringService riskScoringService) {
        this.detector = detector;
        this.signalValidationChain = signalValidationChain;
        this.riskScoringService = riskScoringService;
    }

    public List<SignalEvaluation> handle(List<DetectEarthquakeCommand> commands) {
        List<SignalEvaluation> out = new ArrayList<>(commands.size());
        for (DetectEarthquakeCommand command : commands) {
            out.add(evaluateOne(command));
        }
        return out;
    }

    private SignalEvaluation evaluateOne(DetectEarthquakeCommand command) {
        SeismicSignal signal = new SeismicSignal(
                command.stationId(),
                new GeoLocation(command.latitude(), command.longitude()),
                command.sampleRateHz(),
                command.startTime(),
                command.amplitudes());

        try {
            signalValidationChain.validate(signal);
        } catch (SignalRejectedException rejected) {
            // Unusable window — record it as "no detection" and keep the sweep going.
            return new SignalEvaluation(DetectionResult.notDetected(0.0, 0.0), null);
        }

        DetectionResult result = detector.analyze(signal);
        if (!result.detected()) {
            return new SignalEvaluation(result, null);
        }

        // Score a TRANSIENT earthquake (constructed, never saved) only to read its level.
        Earthquake transientQuake = Earthquake.register(
                new Magnitude(result.estimatedMagnitude()),
                DEFAULT_DEPTH_KM,
                signal.location(),
                signal.stationId(),
                result.triggeredAt());
        RiskScore score = riskScoringService.assess(transientQuake);
        return new SignalEvaluation(result, score.level());
    }
}
