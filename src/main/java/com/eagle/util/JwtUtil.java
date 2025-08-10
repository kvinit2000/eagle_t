package com.eagle.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JwtUtil {
    private static final String SECRET_KEY = "replace_with_a_very_long_random_secret_key_here_1234567890";
    private static final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    public static Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    public static boolean validateToken(String token) {
        try {
            Jws<Claims> claims = parseToken(token);
            Date expiration = claims.getBody().getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            // Signature invalid, token malformed, expired, etc.
            return false;
        }
    }

    public static String extractUsername(String token) {
        return parseToken(token).getBody().getSubject();
    }
}
