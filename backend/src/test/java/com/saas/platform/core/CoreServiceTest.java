package com.saas.platform.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saas.platform.common.ConflictException;
import com.saas.platform.platform.TenantRepository;
import com.saas.platform.security.AuthUserPrincipal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

class CoreServiceTest {

    @Test
    void ownerCanCreateDomainRoleAndPasswordIsEncoded() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(users.existsByEmail("staff@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("encoded");
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(5L);
            return user;
        });
        CoreService service = service(users, mock(SettingsRepository.class), mock(TenantRepository.class), encoder);

        UserResponse created = service.createUser(
            new UserRequest("Staff", "STAFF@example.com", "WAITER", "password123"),
            principal(1L, "OWNER", "RESTAURANT")
        );

        assertEquals("staff@example.com", created.email());
        assertEquals("WAITER", created.role());
        verify(encoder).encode("password123");
    }

    @Test
    void managerCannotCreateManagerAndRestaurantCannotCreateRetailRole() {
        UserRepository users = mock(UserRepository.class);
        CoreService service = service(users, mock(SettingsRepository.class), mock(TenantRepository.class), mock(PasswordEncoder.class));

        assertThrows(
            AccessDeniedException.class,
            () -> service.createUser(
                new UserRequest("Manager", "manager@example.com", "MANAGER", "password123"),
                principal(2L, "MANAGER", "RESTAURANT")
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> service.createUser(
                new UserRequest("Stock", "stock@example.com", "STOCK_KEEPER", "password123"),
                principal(1L, "OWNER", "RESTAURANT")
            )
        );
        verify(users, never()).save(any(User.class));
    }

    @Test
    void managerCanCreateAllowedStaffButCannotEditManagementAccounts() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(users.existsByEmail("cashier@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("encoded");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CoreService service = service(users, mock(SettingsRepository.class), mock(TenantRepository.class), encoder);

        service.createUser(
            new UserRequest("Cashier", "cashier@example.com", "CASHIER", "password123"),
            principal(2L, "MANAGER", "RETAIL")
        );

        User owner = new User();
        owner.setId(1L);
        owner.setRole("OWNER");
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        assertThrows(
            AccessDeniedException.class,
            () -> service.setUserActive(1L, false, principal(2L, "MANAGER", "RESTAURANT"))
        );
        verify(users, never()).save(owner);
    }

    @Test
    void cannotDeactivateOrDemoteCurrentOwner() {
        UserRepository users = mock(UserRepository.class);
        User owner = new User();
        owner.setId(1L);
        owner.setRole("OWNER");
        owner.setActive(true);
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        CoreService service = service(users, mock(SettingsRepository.class), mock(TenantRepository.class), mock(PasswordEncoder.class));
        AuthUserPrincipal principal = principal(1L, "OWNER", "RESTAURANT");

        assertThrows(AccessDeniedException.class, () -> service.setUserActive(1L, false, principal));
        assertThrows(
            AccessDeniedException.class,
            () -> service.updateUser(1L, new UserRequest("Owner", "owner@example.com", "MANAGER", null), principal)
        );
        verify(users, never()).save(owner);
    }

    @Test
    void duplicateEmailIsReportedAsConflict() {
        UserRepository users = mock(UserRepository.class);
        when(users.existsByEmail("owner@example.com")).thenReturn(true);
        CoreService service = service(users, mock(SettingsRepository.class), mock(TenantRepository.class), mock(PasswordEncoder.class));

        assertThrows(
            ConflictException.class,
            () -> service.createUser(
                new UserRequest("Staff", "owner@example.com", "CASHIER", "password123"),
                principal(1L, "OWNER", "RETAIL")
            )
        );
    }

    private CoreService service(
        UserRepository users,
        SettingsRepository settings,
        TenantRepository tenants,
        PasswordEncoder encoder) {
        return new CoreService(users, settings, tenants, encoder);
    }

    private AuthUserPrincipal principal(long userId, String role, String businessType) {
        return new AuthUserPrincipal(userId, 10L, "actor@example.com", role, businessType);
    }
}
