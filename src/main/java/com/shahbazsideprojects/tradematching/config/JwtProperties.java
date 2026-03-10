package com.shahbazsideprojects.tradematching.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT configuration. Bind to jwt.secret and jwt.expiration-ms in application properties.
 * Reference this config wherever JWT settings are needed (e.g. JwtUtil).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Secret key for signing tokens (min 32 characters for HS256).
     */
    private String secret;

    /**
     * Token TTL in milliseconds. Default 86400000 (24 hours).
     */
    private long expirationMs = 86400000L;
}
