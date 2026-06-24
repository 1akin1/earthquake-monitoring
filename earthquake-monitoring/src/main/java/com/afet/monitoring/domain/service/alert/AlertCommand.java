package com.afet.monitoring.domain.service.alert;

/**
 * Command Pattern. Encapsulates ONE alert action (notify responders, broadcast a public
 * warning, record a low-risk note…) as an object with a uniform interface, so the caller
 * that triggers it ({@code AlertEventListener}) is decoupled from how it is carried out.
 *
 * <p>Each command knows how to {@link #execute()} itself and how to {@link #undo()} the
 * effect, which is what lets the {@link AlertDispatcher} retry a flaky action and roll a
 * partially-applied batch back. Pure domain — the actual side effect happens through an
 * injected {@link AlertChannel} (the receiver), never inside the command's own code.
 */
public interface AlertCommand {

    /** Carry out the action. May throw if the underlying channel fails transiently. */
    void execute();

    /** Reverse the effect of a previous successful {@link #execute()} (best-effort). */
    void undo();

    /** Human-readable description, used in logs and error messages. */
    String description();
}
