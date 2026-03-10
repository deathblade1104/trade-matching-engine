package com.shahbazsideprojects.tradematching.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Request-scoped auth context (session context). Set by JwtFilter when a valid JWT is present.
 * Inject this in controllers or services to get the current user without passing principal through every layer.
 */
@Getter
@Setter
@Component
@RequestScope
public class AuthContext {

    private UserPrincipal principal;

    /**
     * Current user id, or null if not authenticated.
     */
    public Long getCurrentUserId() {
        return principal != null ? principal.id() : null;
    }

    public boolean isAuthenticated() {
        return principal != null;
    }
}
