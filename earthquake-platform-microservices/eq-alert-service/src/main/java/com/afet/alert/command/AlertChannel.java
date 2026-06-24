package com.afet.alert.command;

/** The Command's receiver: actually delivers (and can retract) an alert. */
public interface AlertChannel {
    String send(String message);
    void retract(String handle);
}
