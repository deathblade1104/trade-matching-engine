package com.shahbazsideprojects.tradematching.security;

import com.shahbazsideprojects.tradematching.service.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_RESPONSE_JSON = "{\"message\":\"Token is blacklisted\"}";

    private final JwtUtil jwtUtil;
    private final AuthService authService;
    private final AuthContext authContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token)) {
            try {
                if (authService.isBlacklisted(token)) {
                    sendUnauthorized(response, BLACKLIST_RESPONSE_JSON);
                    return;
                }
                Claims claims = jwtUtil.parseToken(token);
                if (claims != null) {
                    UserPrincipal principal = toPrincipal(claims);
                    var auth = new UsernamePasswordAuthenticationToken(principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    authContext.setPrincipal(principal);
                }
            } catch (Exception e) {
                log.debug("Invalid or expired JWT: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private UserPrincipal toPrincipal(Claims claims) {
        Long id = Long.parseLong(claims.getSubject());
        String email = claims.get("email", String.class);
        String name = claims.get("name", String.class);
        return new UserPrincipal(id, email, name);
    }

    private void sendUnauthorized(HttpServletResponse response, String body) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=" + StandardCharsets.UTF_8.name());
        response.getWriter().write(body);
    }
}
