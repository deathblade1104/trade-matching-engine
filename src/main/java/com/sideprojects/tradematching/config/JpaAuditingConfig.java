package com.sideprojects.tradematching.config;

import com.sideprojects.tradematching.security.AuthContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Enables JPA auditing so created_by and updated_by are set to the signed-in user's email.
 * When there is no request context (e.g. scheduler), they remain null.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware(AuthContext authContext) {
        return () -> Optional.ofNullable(authContext.getPrincipal())
                .map(principal -> principal.email());
    }
}
