package com.afet.alert.command;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Command invoker: runs commands with retry, and executes a batch atomically — if any
 * command fails after its retries, the ones already executed are undone (LIFO) and the
 * failure rethrown. Holds no mutable state, so a single instance is concurrency-safe.
 */
public class AlertDispatcher {

    private final int maxAttempts;

    public AlertDispatcher(int maxAttempts) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        this.maxAttempts = maxAttempts;
    }

    public List<AlertCommand> dispatchBatch(List<AlertCommand> commands) {
        Deque<AlertCommand> done = new ArrayDeque<>();
        for (AlertCommand command : commands) {
            try {
                executeWithRetry(command);
                done.push(command);
            } catch (AlertDispatchException failure) {
                while (!done.isEmpty()) done.pop().undo();
                throw failure;
            }
        }
        return List.copyOf(done);
    }

    public void dispatch(AlertCommand command) { executeWithRetry(command); }

    private void executeWithRetry(AlertCommand command) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try { command.execute(); return; }
            catch (RuntimeException ex) { last = ex; }
        }
        throw new AlertDispatchException(command.description(), maxAttempts, last);
    }
}
