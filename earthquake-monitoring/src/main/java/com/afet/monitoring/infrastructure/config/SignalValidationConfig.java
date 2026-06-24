package com.afet.monitoring.infrastructure.config;

import com.afet.monitoring.domain.service.validation.ClippingValidator;
import com.afet.monitoring.domain.service.validation.DeadChannelValidator;
import com.afet.monitoring.domain.service.validation.FiniteAmplitudeValidator;
import com.afet.monitoring.domain.service.validation.SignalValidator;
import com.afet.monitoring.domain.service.validation.WindowLengthValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The ONLY Spring-aware place for the signal-validation chain. The validators are pure
 * domain (Chain of Responsibility); here they are linked, in order, into one chain whose
 * head is exposed as the single {@link SignalValidator} bean the detection use case calls.
 *
 * <p>The {@code linkTo(...).linkTo(...)} fluent assembly makes the chain order explicit
 * and readable: integrity → analysability → sensor health → saturation. Cheap, structural
 * checks run first; situational ones last. Adding a rule = a new validator + one
 * {@code linkTo} line here.
 */
@Configuration
public class SignalValidationConfig {

    /** LTA window in {@code DetectionConfig} is 100 samples; the detector needs one more. */
    private static final int MIN_SAMPLES = 101;

    /** Reject if the std dev is below this — only genuinely flat (dead-channel) traces. */
    private static final double MIN_STD_DEV = 1e-6;

    /** Reject if more than this fraction of samples sit exactly at the peak (clipping). */
    private static final double MAX_PEAK_FRACTION = 0.20;

    @Bean
    SignalValidator signalValidationChain() {
        FiniteAmplitudeValidator head = new FiniteAmplitudeValidator();
        head.linkTo(new WindowLengthValidator(MIN_SAMPLES))
                .linkTo(new DeadChannelValidator(MIN_STD_DEV))
                .linkTo(new ClippingValidator(MAX_PEAK_FRACTION));
        return head; // head of the chain
    }
}
