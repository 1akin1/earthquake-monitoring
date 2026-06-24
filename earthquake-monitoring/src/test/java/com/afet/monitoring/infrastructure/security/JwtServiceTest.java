package com.afet.monitoring.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the JDK-only JWT service: a sign→verify roundtrip, the expiry boundary, and
 * rejection of tampered, wrongly-signed, malformed and missing tokens.
 */
@DisplayName("JwtService issues and validates HS256 tokens with no third-party library")
class JwtServiceTest {

    private static final String SECRET = "super-secret-signing-key-0123456789";
    private final JwtService jwt = new JwtService(SECRET, Duration.ofHours(1));
    private final Instant now = Instant.parse("2026-06-22T00:00:00Z");

    @Test
    @DisplayName("a token round-trips its subject and role")
    void roundtrip() {
        String token = jwt.issueAt("alice", Role.ADMIN, now);
        assertThat(token.split("\\.")).hasSize(3);

        JwtService.Claims claims = jwt.parseAt(token, now.plusSeconds(60));
        assertThat(claims.subject()).isEqualTo("alice");
        assertThat(claims.role()).isEqualTo(Role.ADMIN);
        assertThat(claims.role().authority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("a token is valid up to, and invalid at, its expiry")
    void expiry_boundary() {
        String token = jwt.issueAt("alice", Role.SCIENTIST, now); // ttl 1h -> exp now+3600

        assertThat(jwt.parseAt(token, now.plusSeconds(3599)).role()).isEqualTo(Role.SCIENTIST);
        assertThatThrownBy(() -> jwt.parseAt(token, now.plusSeconds(3601)))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("a tampered payload fails the signature check")
    void tampered_payload_rejected() {
        String token = jwt.issueAt("alice", Role.PUBLIC, now);
        String[] parts = token.split("\\.");
        String forged = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"alice\",\"role\":\"ADMIN\",\"iat\":1,\"exp\":9999999999}".getBytes());
        String tampered = parts[0] + "." + forged + "." + parts[2];

        assertThatThrownBy(() -> jwt.parseAt(tampered, now))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("bad signature");
    }

    @Test
    @DisplayName("a token signed with another secret is rejected")
    void wrong_secret_rejected() {
        String token = jwt.issueAt("alice", Role.ADMIN, now);
        JwtService other = new JwtService("a-totally-different-key-9876543210", Duration.ofHours(1));

        assertThatThrownBy(() -> other.parseAt(token, now.plusSeconds(60)))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("bad signature");
    }

    @Test
    @DisplayName("malformed and missing tokens are rejected")
    void malformed_rejected() {
        assertThatThrownBy(() -> jwt.parseAt("not-a-jwt", now))
                .isInstanceOf(JwtException.class).hasMessageContaining("malformed");
        assertThatThrownBy(() -> jwt.parseAt(null, now))
                .isInstanceOf(JwtException.class).hasMessageContaining("missing token");
    }

    @Test
    @DisplayName("a too-short secret is refused at construction")
    void weak_secret_refused() {
        assertThatThrownBy(() -> new JwtService("short", Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
