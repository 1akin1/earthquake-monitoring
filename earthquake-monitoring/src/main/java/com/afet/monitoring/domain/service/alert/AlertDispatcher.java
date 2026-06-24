package com.afet.monitoring.domain.service.alert;

import com.afet.monitoring.domain.exception.AlertDispatchException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * The Command <i>invoker</i>. Runs alert commands without knowing what any of them does,
 * adding two cross-cutting behaviours on top of the bare {@link AlertCommand} interface:
 *
 * <ul>
 *   <li><b>Retry</b> — a command's {@link AlertCommand#execute()} is attempted up to
 *       {@code maxAttempts} times, so a transient channel failure doesn't lose an alert.</li>
 *   <li><b>Undo / rollback</b> — {@link #dispatchBatch(List)} executes a group atomically:
 *       if any command still fails after its retries, every command already executed in the
 *       batch is {@link AlertCommand#undo() undone} in LIFO order before the failure is
 *       rethrown, so the batch never lands half-applied.</li>
 * </ul>
 *
 * <p>Holds no mutable state (only the {@code maxAttempts} setting); the per-batch history
 * is local to each call, so a single instance is safe to share across concurrent events.
 */
public class AlertDispatcher {

    private final int maxAttempts;

    public AlertDispatcher(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.maxAttempts = maxAttempts;
    }

    /**
     * Execute every command in order. On a terminal failure, undo the ones already done
     * (LIFO) and rethrow. Returns the executed commands, most-recent first, so a caller
     * can undo them later if it wants.
     */
    public List<AlertCommand> dispatchBatch(List<AlertCommand> commands) {
        Deque<AlertCommand> done = new ArrayDeque<>();
        for (AlertCommand command : commands) {
            try {
                executeWithRetry(command);
                done.push(command);
            } catch (AlertDispatchException failure) {
                while (!done.isEmpty()) {
                    done.pop().undo();
                }
                throw failure;
            }
        }
        return List.copyOf(done);
    }

    /** Execute a single command with retry; no rollback (nothing else to undo). */
    public void dispatch(AlertCommand command) {
        executeWithRetry(command);
    }

    private void executeWithRetry(AlertCommand command) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                command.execute();
                return;
            } catch (RuntimeException ex) {
                last = ex; // transient — try again
            }
        }
        throw new AlertDispatchException(command.description(), maxAttempts, last);
    }
}
