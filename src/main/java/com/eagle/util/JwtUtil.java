package com.eagle.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;

public class JwtUtil {
    // NOTE: for demo only; move to env var and make 32+ chars
    private static final String SECRET = "change-me-please-32+chars-secret-key-123";
    private static final long   TTL_MS = 3600_000; // 1 hour

    public static String issue(String subject){ // subject = username
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + TTL_MS))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }
    public static long ttlSeconds(){ return TTL_MS / 1000; }
}
