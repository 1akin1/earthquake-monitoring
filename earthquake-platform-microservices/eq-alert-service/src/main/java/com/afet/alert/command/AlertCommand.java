package com.afet.alert.command;

/**
 * Command Pattern. Encapsulates one alert action with execute/undo so the
 * {@link AlertDispatcher} can retry it and roll a partial batch back.
 */
public interface AlertCommand {
    void execute();
    void undo();
    String description();
}
