package com.eagle.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

public class JwtUtil {
    private static final String SECRET = System.getenv()
            .getOrDefault("JWT_SECRET", "change-this-secret-key-at-least-32-bytes-long!!");

    private static final long TTL_SECONDS = Long.parseLong(
            System.getenv().getOrDefault("JWT_TTL_SECONDS", "3600")
    );

    private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /** Issue a JWT for the given subject (e.g., username). */
    public static String issue(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(TTL_SECONDS)))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Optional: verify & return subject. Throws if invalid/expired. */
    public static String verify(String token) {
        Jws<Claims> j = Jwts.parserBuilder().setSigningKey(KEY).build().parseClaimsJws(token);
        return j.getBody().getSubject();
    }

    /** Expose configured TTL in seconds (for your LoginResponse). */
    public static long ttlSeconds() {
        return TTL_SECONDS;
    }
}
