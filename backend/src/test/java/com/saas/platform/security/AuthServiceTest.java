package com.saas.platform.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saas.platform.core.User;
import com.saas.platform.core.UserRepository;
import com.saas.platform.platform.PlatformAdminRepository;
import com.saas.platform.platform.Tenant;
import com.saas.platform.platform.TenantRepository;
import com.saas.platform.tenancy.TenantContext;
import com.saas.platform.tenancy.TenantDataSourceRegistry;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void tenantLoginLoadsUserFromTenantInTenantContext() {
        TenantRepository tenants = mock(TenantRepository.class);
        UserRepository users = mock(UserRepository.class);
        PlatformAdminRepository admins = mock(PlatformAdminRepository.class);
        TenantDataSourceRegistry registry = mock(TenantDataSourceRegistry.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = new JwtService("local-test-secret-that-is-at-least-32-bytes-long");
        Tenant tenant = new Tenant();
        tenant.setId(12L);
        tenant.setCode("sample-shop");
        tenant.setName("Sample Shop");
        tenant.setBusinessType("RETAIL");
        tenant.setDbName("tenant_12");
        tenant.setStatus("ACTIVE");
        User owner = new User();
        owner.setId(7L);
        owner.setName("Owner");
        owner.setEmail("owner@example.com");
        owner.setRole("OWNER");
        owner.setPasswordHash("hashed-password");
        owner.setActive(true);

        when(tenants.findByCode("sample-shop")).thenReturn(Optional.of(tenant));
        when(users.findByEmail("owner@example.com")).thenAnswer(invocation -> {
            assertEquals(12L, TenantContext.get());
            return Optional.of(owner);
        });
        when(passwordEncoder.matches("password", "hashed-password")).thenReturn(true);
        when(registry.get(12L)).thenReturn(mock(com.zaxxer.hikari.HikariDataSource.class));
        AuthService service = new AuthService(tenants, users, admins, registry, passwordEncoder, jwtService);

        LoginResponse response = service.loginTenant(
            new TenantLoginRequest("sample-shop", "owner@example.com", "password")
        );

        assertEquals("Sample Shop", response.user().businessName());
        assertEquals(12L, jwtService.parse(response.token()).tenantId());
        assertEquals(null, TenantContext.get());
        verify(registry).get(12L);
    }

    @Test
    void unknownTenantAndWrongPasswordUseSameMessage() {
        TenantRepository tenants = mock(TenantRepository.class);
        UserRepository users = mock(UserRepository.class);
        PlatformAdminRepository admins = mock(PlatformAdminRepository.class);
        TenantDataSourceRegistry registry = mock(TenantDataSourceRegistry.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = new JwtService("local-test-secret-that-is-at-least-32-bytes-long");
        Tenant tenant = new Tenant();
        tenant.setId(12L);
        tenant.setStatus("ACTIVE");
        tenant.setDbName("tenant_12");
        tenant.setBusinessType("RETAIL");
        User owner = new User();
        owner.setEmail("owner@example.com");
        owner.setPasswordHash("hashed-password");
        owner.setActive(true);
        when(tenants.findByCode("sample-shop")).thenReturn(Optional.of(tenant));
        when(tenants.findByCode("unknown-shop")).thenReturn(Optional.empty());
        when(registry.get(12L)).thenReturn(mock(com.zaxxer.hikari.HikariDataSource.class));
        when(users.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);
        AuthService service = new AuthService(tenants, users, admins, registry, passwordEncoder, jwtService);

        InvalidCredentialsException wrongCode = assertThrows(
            InvalidCredentialsException.class,
            () -> service.loginTenant(new TenantLoginRequest("unknown-shop", "owner@example.com", "password"))
        );
        InvalidCredentialsException wrongPassword = assertThrows(
            InvalidCredentialsException.class,
            () -> service.loginTenant(new TenantLoginRequest("sample-shop", "owner@example.com", "wrong-password"))
        );

        assertEquals(wrongCode.getMessage(), wrongPassword.getMessage());
    }

    @Test
    void suspendedTenantLoginReturnsClearMessageWithoutOpeningDatasource() {
        TenantRepository tenants = mock(TenantRepository.class);
        Tenant suspended = new Tenant();
        suspended.setCode("sample-shop");
        suspended.setStatus("SUSPENDED");
        when(tenants.findByCode("sample-shop")).thenReturn(Optional.of(suspended));
        TenantDataSourceRegistry registry = mock(TenantDataSourceRegistry.class);
        AuthService service = new AuthService(
            tenants,
            mock(UserRepository.class),
            mock(PlatformAdminRepository.class),
            registry,
            mock(PasswordEncoder.class),
            new JwtService("local-test-secret-that-is-at-least-32-bytes-long")
        );

        InvalidCredentialsException error = assertThrows(
            InvalidCredentialsException.class,
            () -> service.loginTenant(new TenantLoginRequest("sample-shop", "owner@example.com", "password"))
        );

        assertEquals("Business workspace is suspended", error.getMessage());
        org.mockito.Mockito.verifyNoInteractions(registry);
    }
}
