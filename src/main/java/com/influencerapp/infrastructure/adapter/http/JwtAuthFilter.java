package com.influencerapp.infrastructure.adapter.http;

import com.influencerapp.domain.model.TenantId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;


/**
 * JWT authentication filter that validates Bearer tokens and extracts tenant identity.
 *
 * @author judaro122
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.contains("/api/auth/") || path.contains("/api/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"type\":\"https://influencerapp/errors/unauthorized\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"JWT token is missing or invalid\",\"instance\":\"" + request.getRequestURI() + "\"}");
            return;
        }
        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String userId = claims.getSubject();
            String tenantIdValue = claims.get("tenantId", String.class);
            if (userId == null || tenantIdValue == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/problem+json");
                response.getWriter().write("{\"type\":\"https://influencerapp/errors/unauthorized\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"JWT token is missing or invalid\",\"instance\":\"" + request.getRequestURI() + "\"}");
                return;
            }
            TenantId tenantId = new TenantId(tenantIdValue);
            Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("tenantId", tenantId);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"type\":\"https://influencerapp/errors/unauthorized\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"JWT token is missing or invalid\",\"instance\":\"" + request.getRequestURI() + "\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/api/auth/") || path.contains("/api/health");
    }
}