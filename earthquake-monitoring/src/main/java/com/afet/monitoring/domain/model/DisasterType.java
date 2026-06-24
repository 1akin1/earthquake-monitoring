package com.afet.monitoring.domain.model;

import com.afet.monitoring.domain.exception.UnsupportedDisasterTypeException;

/**
 * The kinds of disaster the platform can assess. The Factory selects a handler by
 * this type. Keeping it an enum (not a free string inside the domain) means the
 * compiler enforces exhaustiveness and typos can't reach the business logic — the
 * raw client string is parsed once, at the edge, via {@link #from(String)}.
 */
public enum DisasterType {
    EARTHQUAKE,
    FLOOD,
    WILDFIRE;

    /** Parse a client-supplied string (e.g. "earthquake") into a type, case-insensitively. */
    public static DisasterType from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new UnsupportedDisasterTypeException(raw);
        }
        try {
            return DisasterType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new UnsupportedDisasterTypeException(raw);
        }
    }
}
