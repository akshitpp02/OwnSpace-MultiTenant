package com.saas.platform.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.platform.tenancy.TenantContext;
import com.saas.platform.tenancy.TenantDataSourceRegistry;
import com.saas.platform.tenancy.TenantRoutingDataSource;
import com.saas.platform.tenancy.TenantRoutingException;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.MDC;
import static org.mockito.Mockito.doThrow;

class JwtAuthFilterTest {

    private static final String SECRET = "local-test-secret-that-is-at-least-32-bytes-long";

    @AfterEach
    void clearRequestState() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void verifiedTenantTokenSetsContextForTheRequestAndClearsItAfterward() throws Exception {
        JwtService jwtService = new JwtService(SECRET);
        TenantDataSourceRegistry registry = mock(TenantDataSourceRegistry.class);
        when(registry.get(12L)).thenReturn(mock(HikariDataSource.class));
        JwtAuthFilter filter = new JwtAuthFilter(jwtService, registry, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " +
            jwtService.createTenantToken(7L, 12L, "OWNER", "RESTAURANT", "owner@example.com"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            assertEquals(12L, TenantContext.get());
            assertEquals("12", MDC.get("tenantId"));
            AuthUserPrincipal principal = (AuthUserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
            assertEquals(7L, principal.userId());
        };

        filter.doFilter(request, response, chain);

        assertNull(TenantContext.get());
        assertNull(MDC.get("tenantId"));
    }

    @Test
    void authenticatedRequestsRouteToOnlyTheirOwnTenantDataSource() throws Exception {
        JwtService jwtService = new JwtService(SECRET);
        TenantDataSourceRegistry registry = mock(TenantDataSourceRegistry.class);
        HikariDataSource restaurantDataSource = mock(HikariDataSource.class);
        HikariDataSource retailDataSource = mock(HikariDataSource.class);
        java.sql.Connection restaurantConnection = mock(java.sql.Connection.class);
        java.sql.Connection retailConnection = mock(java.sql.Connection.class);
        when(registry.get(12L)).thenReturn(restaurantDataSource);
        when(registry.get(34L)).thenReturn(retailDataSource);
        when(restaurantDataSource.getConnection()).thenReturn(restaurantConnection);
        when(retailDataSource.getConnection()).thenReturn(retailConnection);
        JwtAuthFilter filter = new JwtAuthFilter(jwtService, registry, new ObjectMapper());
        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource(registry);

        MockHttpServletRequest restaurantRequest = tenantRequest(
            jwtService, 12L, "RESTAURANT", "restaurant-owner@example.com");
        filter.doFilter(restaurantRequest, new MockHttpServletResponse(), (request, response) -> {
            assertSame(restaurantConnection, routedConnection(routingDataSource));
        });

        MockHttpServletRequest retailRequest = tenantRequest(
            jwtService, 34L, "RETAIL", "retail-owner@example.com");
        filter.doFilter(retailRequest, new MockHttpServletResponse(), (request, response) -> {
            assertSame(retailConnection, routedConnection(routingDataSource));
        });

        assertNull(TenantContext.get());
    }

    @Test
    void rejectsInvalidTokenInStandardUnauthorizedResponse() throws Exception {
        JwtService jwtService = new JwtService(SECRET);
        JwtAuthFilter filter = new JwtAuthFilter(jwtService, mock(TenantDataSourceRegistry.class), new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            throw new AssertionError("Invalid token must not reach the application");
        };

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals(
            "{\"success\":false,\"message\":\"Invalid or expired token\",\"data\":null}",
            response.getContentAsString()
        );
    }

    @Test
    void rejectsExistingTenantTokenWhenItsDatasourceHasBeenRemoved() throws Exception {
        JwtService jwtService = new JwtService(SECRET);
        TenantDataSourceRegistry registry = mock(TenantDataSourceRegistry.class);
        doThrow(new TenantRoutingException("Tenant 12 is not active or is not registered"))
            .when(registry).get(12L);
        JwtAuthFilter filter = new JwtAuthFilter(jwtService, registry, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " +
            jwtService.createTenantToken(7L, 12L, "OWNER", "RESTAURANT", "owner@example.com"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            throw new AssertionError("Suspended tenant token must not reach the application");
        };

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertEquals(
            "{\"success\":false,\"message\":\"Tenant workspace is suspended, inactive, or unavailable\",\"data\":null}",
            response.getContentAsString()
        );
    }

    private MockHttpServletRequest tenantRequest(
        JwtService jwtService, long tenantId, String businessType, String email) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " +
            jwtService.createTenantToken(1L, tenantId, "OWNER", businessType, email));
        return request;
    }

    private java.sql.Connection routedConnection(TenantRoutingDataSource routingDataSource) throws ServletException {
        try {
            return routingDataSource.getConnection();
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }
}
