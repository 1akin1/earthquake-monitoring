package com.afet.monitoring.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * A minimal JWT (HS256) implementation built on the JDK alone — {@link Mac} for the
 * HMAC-SHA256 signature and {@link Base64} URL encoding — so the platform issues and
 * verifies tokens without pulling in a third-party JWT library.
 *
 * <p>Token shape is the standard three dot-separated Base64URL parts:
 * {@code base64url(header).base64url(payload).base64url(signature)}. Claims carried:
 * {@code sub} (username), {@code role}, {@code iat}, {@code exp}.
 *
 * <p>{@link #parse(String)} verifies the signature (constant-time) and the expiry before
 * returning the claims; anything wrong throws {@link JwtException}. Pure logic, no Spring —
 * which is also what makes it straightforward to unit-test.
 */
public class JwtService {

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private static final Pattern SUB  = Pattern.compile("\"sub\":\"([^\"]*)\"");
    private static final Pattern ROLE = Pattern.compile("\"role\":\"([^\"]*)\"");
    private static final Pattern EXP  = Pattern.compile("\"exp\":(\\d+)");

    private final byte[] secret;
    private final Duration ttl;

    public JwtService(String secret, Duration ttl) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalArgumentException("JWT secret must be at least 16 chars");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttl = ttl;
    }

    /** Issue a token for {@code subject} carrying {@code role}, valid for the configured TTL. */
    public String issue(String subject, Role role) {
        return issueAt(subject, role, Instant.now());
    }

    /** Visible for testing: issue with an explicit "now" so expiry is deterministic. */
    String issueAt(String subject, Role role, Instant now) {
        long iat = now.getEpochSecond();
        long exp = now.plus(ttl).getEpochSecond();
        String payloadJson = "{\"sub\":\"" + subject + "\",\"role\":\"" + role.name()
                + "\",\"iat\":" + iat + ",\"exp\":" + exp + "}";

        String headerB64 = B64.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = B64.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = headerB64 + "." + payloadB64;
        String signatureB64 = B64.encodeToString(hmac(signingInput));

        return signingInput + "." + signatureB64;
    }

    /** Parse and fully validate a token. Throws {@link JwtException} if it isn't usable. */
    public Claims parse(String token) {
        return parseAt(token, Instant.now());
    }

    /** Visible for testing: validate against an explicit "now". */
    Claims parseAt(String token, Instant now) {
        if (token == null) {
            throw new JwtException("missing token");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtException("malformed token");
        }
        String signingInput = parts[0] + "." + parts[1];
        byte[] expected = hmac(signingInput);
        byte[] presented = decode(parts[2]);
        if (!MessageDigest.isEqual(expected, presented)) {   // constant-time compare
            throw new JwtException("bad signature");
        }

        String payload = new String(decode(parts[1]), StandardCharsets.UTF_8);
        long exp = longClaim(EXP, payload, "exp");
        if (now.getEpochSecond() >= exp) {
            throw new JwtException("token expired");
        }
        String subject = stringClaim(SUB, payload, "sub");
        Role role = parseRole(stringClaim(ROLE, payload, "role"));
        return new Claims(subject, role, Instant.ofEpochSecond(exp));
    }

    private byte[] hmac(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new JwtException("cannot sign token: " + e.getMessage());
        }
    }

    private static byte[] decode(String part) {
        try {
            return B64D.decode(part);
        } catch (IllegalArgumentException e) {
            throw new JwtException("malformed token segment");
        }
    }

    private static String stringClaim(Pattern p, String payload, String name) {
        Matcher m = p.matcher(payload);
        if (!m.find()) {
            throw new JwtException("missing claim: " + name);
        }
        return m.group(1);
    }

    private static long longClaim(Pattern p, String payload, String name) {
        Matcher m = p.matcher(payload);
        if (!m.find()) {
            throw new JwtException("missing claim: " + name);
        }
        return Long.parseLong(m.group(1));
    }

    private static Role parseRole(String raw) {
        try {
            return Role.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new JwtException("unknown role: " + raw);
        }
    }

    /** Validated token claims. */
    public record Claims(String subject, Role role, Instant expiresAt) {}
}
