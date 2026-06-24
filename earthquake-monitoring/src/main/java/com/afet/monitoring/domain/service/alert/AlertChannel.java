package com.afet.monitoring.domain.service.alert;

/**
 * The Command's <i>receiver</i>: the thing that actually delivers (and can retract) an
 * alert. Modelled as a port so the domain stays framework-free — the real implementation
 * (logging, SMS, push, siren…) lives in infrastructure. A command calls
 * {@link #send(String)} to act and remembers the returned handle so {@link #retract(String)}
 * can undo it.
 */
public interface AlertChannel {

    /** Deliver {@code message}; returns an opaque handle identifying this delivery. */
    String send(String message);

    /** Retract a previous delivery identified by {@code handle} (the undo side effect). */
    void retract(String handle);
}
