package com.afet.monitoring.infrastructure.security;

/**
 * The three access roles from the roadmap. Ordered least → most privileged so policies can
 * reason about hierarchy if needed.
 *
 * <ul>
 *   <li>{@code PUBLIC} — read-only access to earthquakes, reports and stats.</li>
 *   <li>{@code SCIENTIST} — may run analysis: detection, import, assessment, a cycle.</li>
 *   <li>{@code ADMIN} — everything, including destructive operations (delete).</li>
 * </ul>
 */
public enum Role {
    PUBLIC,
    SCIENTIST,
    ADMIN;

    /** Spring Security authority name convention is {@code ROLE_<NAME>}. */
    public String authority() {
        return "ROLE_" + name();
    }
}
