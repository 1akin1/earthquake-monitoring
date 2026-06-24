package com.afet.monitoring.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A tiny in-memory user directory for the demo: a single admin account with a
 * BCrypt-hashed password. Stands in for a real user service / identity provider — the
 * rest of the security stack (JWT issuing, the filter, the authorization rules) is
 * unchanged whether credentials come from here or from a database later.
 *
 * <p>The console is open (read-only) without logging in; this account exists only to
 * unlock the privileged operations (feed import, detection, delete).
 *
 * <p>Demo credentials: {@code admin/admin123}.
 */
@Component
public class DemoUserStore {

    private record Account(String passwordHash, Role role) {}

    private final Map<String, Account> accounts = new HashMap<>();
    private final PasswordEncoder encoder;

    public DemoUserStore(PasswordEncoder encoder) {
        this.encoder = encoder;
        // Single admin account — the console is open (read-only) without login; this
        // account unlocks the privileged operations (import, detection, delete).
        accounts.put("admin", new Account(encoder.encode("admin123"), Role.ADMIN));
    }

    /** Return the user's role if the credentials match, else empty. */
    public Optional<Role> authenticate(String username, String rawPassword) {
        Account account = accounts.get(username);
        if (account != null && encoder.matches(rawPassword, account.passwordHash())) {
            return Optional.of(account.role());
        }
        return Optional.empty();
    }
}
