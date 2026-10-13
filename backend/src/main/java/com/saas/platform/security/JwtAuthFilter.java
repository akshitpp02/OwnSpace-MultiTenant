package com.saas.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.platform.common.ApiResponse;
import com.saas.platform.tenancy.TenantContext;
import com.saas.platform.tenancy.TenantDataSourceRegistry;
import com.saas.platform.tenancy.TenantRoutingException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TenantDataSourceRegistry tenantDataSourceRegistry;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(
        JwtService jwtService,
        TenantDataSourceRegistry tenantDataSourceRegistry,
        ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.tenantDataSourceRegistry = tenantDataSourceRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        MDC.remove("tenantId");
        try {
            AuthUserPrincipal principal = null;
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && !authorization.isBlank()) {
                try {
                    principal = parsePrincipal(authorization);
                } catch (JwtException | IllegalArgumentException ex) {
                    SecurityContextHolder.clearContext();
                    writeError(response, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
                    return;
                }
            }

            if (principal != null && principal.tenantId() != null) {
                MDC.put("tenantId", principal.tenantId().toString());
                try {
                    tenantDataSourceRegistry.get(principal.tenantId());
                } catch (TenantRoutingException ex) {
                    SecurityContextHolder.clearContext();
                    writeError(response, HttpStatus.FORBIDDEN, "Tenant workspace is suspended, inactive, or unavailable");
                    return;
                }
                TenantContext.set(principal.tenantId());
            }

            if (principal != null) {
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))
                    );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
        }
    }

    private AuthUserPrincipal parsePrincipal(String authorization) {
        if (!authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            throw new JwtException("Bearer token is missing");
        }
        return jwtService.parse(authorization.substring(7).trim());
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(message));
    }
}
