package com.afet.monitoring.domain.service.validation;

import com.afet.monitoring.domain.exception.SignalRejectedException;
import com.afet.monitoring.domain.model.SeismicSignal;

/**
 * Chain of Responsibility Pattern. Each link checks ONE rule about a raw
 * {@link SeismicSignal} and then passes the signal to the next link. A link that finds a
 * problem calls {@link #reject(String)} (throwing {@link SignalRejectedException}), which
 * short-circuits the rest of the chain — so the first failing rule wins and no later rule
 * runs. If every link passes, the signal is accepted.
 *
 * <p>This is the "validation chain" flavour of CoR (cf. servlet filters, the Spring
 * Security filter chain): unlike the textbook "exactly one handler handles it" form, here
 * <i>every</i> link runs in turn until one rejects. Adding a rule = a new subclass plus
 * one {@code linkTo(...)} in the assembly; existing links never change (OCP).
 *
 * <p>{@link #validate(SeismicSignal)} is {@code final}: a subclass cannot change the
 * "run my check, then delegate to next" mechanic — it only supplies its own
 * {@link #check(SeismicSignal)}. Pure domain, no framework.
 */
public abstract class SignalValidator {

    private SignalValidator next;

    /**
     * Append {@code next} after this link and return it, so links read as a fluent chain:
     * {@code head.linkTo(b).linkTo(c)}.
     */
    public SignalValidator linkTo(SignalValidator next) {
        this.next = next;
        return next;
    }

    /** Run this link's rule, then hand off to the next link (if any). Do not override. */
    public final void validate(SeismicSignal signal) {
        check(signal);                 // throws SignalRejectedException to reject
        if (next != null) {
            next.validate(signal);
        }
    }

    /** This link's single rule. Call {@link #reject(String)} to stop the chain. */
    protected abstract void check(SeismicSignal signal);

    /** Reject the signal, attributing the failure to this validator. */
    protected final void reject(String reason) {
        throw new SignalRejectedException(getClass().getSimpleName(), reason);
    }
}
