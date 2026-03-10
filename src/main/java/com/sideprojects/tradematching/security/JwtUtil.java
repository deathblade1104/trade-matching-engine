package com.sideprojects.tradematching.security;

import com.sideprojects.tradematching.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final int MIN_SECRET_LENGTH = 32;

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException("jwt.secret must be at least 32 characters");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String email, String name) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("name", name)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Parses and validates the token. Returns null if token is blank or invalid.
     */
    public Claims parseToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isExpired(String token) {
        if (token == null || token.isBlank()) {
            return true;
        }
        try {
            Claims claims = parseToken(token);
            return claims != null && claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Returns token expiration date, or null if token is invalid or expired.
     */
    public Date getExpiration(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = parseToken(token);
            return claims != null ? claims.getExpiration() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
