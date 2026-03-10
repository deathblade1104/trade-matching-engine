package com.sideprojects.tradematching.security;

/**
 * Request principal: current user identity from JWT. Held in AuthContext and set by JwtFilter.
 * No longer implements UserDetails; we only need id/email/name and use AuthContext for access.
 */
public record UserPrincipal(Long id, String email, String name) {}
