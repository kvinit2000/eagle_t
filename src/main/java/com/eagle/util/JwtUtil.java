package com.eagle.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JwtUtil {

    // Make sure this is at least 32 bytes for HS256. Replace with a strong random secret.
    private static final String SECRET_KEY = "change_me_to_a_long_random_secret_at_least_32_bytes_long!";
    private static final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // Token lifetime (seconds)
    public static int ttlSeconds() {
        return 3600; // 1 hour
    }

    /** Issue a JWT for the given username as the subject. */
    public static String issue(String username) {
        long now = System.currentTimeMillis();
        Date iat = new Date(now);
        Date exp = new Date(now + (ttlSeconds() * 1000L));

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(iat)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Parse and verify signature; throws on invalid tokens. */
    public static Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    /** Safe boolean validation: signature + not expired. */
    public static boolean validateToken(String token) {
        try {
            Jws<Claims> claims = parseToken(token);
            Date expiration = claims.getBody().getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false; // malformed, bad signature, or expired
        }
    }

    /** Convenience: pull the subject (username) from a valid token. */
    public static String extractUsername(String token) {
        return parseToken(token).getBody().getSubject();
    }
}
