package com.saas.platform.security;

import com.saas.platform.core.User;
import com.saas.platform.core.UserRepository;
import com.saas.platform.platform.PlatformAdmin;
import com.saas.platform.platform.PlatformAdminRepository;
import com.saas.platform.platform.Tenant;
import com.saas.platform.platform.TenantRepository;
import com.saas.platform.tenancy.TenantContext;
import com.saas.platform.tenancy.TenantDataSourceRegistry;
import com.saas.platform.tenancy.TenantRoutingException;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String INVALID_TENANT_CREDENTIALS = "Invalid business code, email, or password";
    private static final String INVALID_PLATFORM_CREDENTIALS = "Invalid email or password";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final TenantDataSourceRegistry tenantDataSourceRegistry;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        TenantRepository tenantRepository,
        UserRepository userRepository,
        PlatformAdminRepository platformAdminRepository,
        TenantDataSourceRegistry tenantDataSourceRegistry,
        PasswordEncoder passwordEncoder,
        JwtService jwtService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.platformAdminRepository = platformAdminRepository;
        this.tenantDataSourceRegistry = tenantDataSourceRegistry;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse loginTenant(TenantLoginRequest request) {
        String code = request.businessCode().trim();
        Tenant tenant = tenantRepository.findByCode(code)
            .orElseThrow(() -> new InvalidCredentialsException(INVALID_TENANT_CREDENTIALS));
        if ("SUSPENDED".equals(tenant.getStatus())) {
            throw new InvalidCredentialsException("Business workspace is suspended");
        }
        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new InvalidCredentialsException(INVALID_TENANT_CREDENTIALS);
        }

        try {
            tenantDataSourceRegistry.get(tenant.getId());
        } catch (TenantRoutingException ex) {
            throw new InvalidCredentialsException(INVALID_TENANT_CREDENTIALS);
        }

        TenantContext.set(tenant.getId());
        try {
            User user = userRepository.findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .filter(User::isActive)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_TENANT_CREDENTIALS));

            AuthenticatedUserResponse responseUser = toTenantResponse(user, tenant);
            String token = jwtService.createTenantToken(
                user.getId(),
                tenant.getId(),
                user.getRole(),
                tenant.getBusinessType(),
                user.getEmail()
            );
            return new LoginResponse(token, responseUser);
        } finally {
            TenantContext.clear();
        }
    }

    public LoginResponse loginPlatformAdmin(PlatformLoginRequest request) {
        PlatformAdmin admin = platformAdminRepository.findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
            .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
            .orElseThrow(() -> new InvalidCredentialsException(INVALID_PLATFORM_CREDENTIALS));

        AuthenticatedUserResponse responseUser = new AuthenticatedUserResponse(
            admin.getId(), null, admin.getEmail(), "PLATFORM_ADMIN", null, "Platform"
        );
        return new LoginResponse(jwtService.createPlatformAdminToken(admin.getId(), admin.getEmail()), responseUser);
    }

    public AuthenticatedUserResponse currentUser(AuthUserPrincipal principal) {
        if (principal.tenantId() == null) {
            PlatformAdmin admin = platformAdminRepository.findById(principal.userId())
                .orElseThrow(() -> new InvalidCredentialsException("Authenticated account no longer exists"));
            return new AuthenticatedUserResponse(
                admin.getId(), null, admin.getEmail(), "PLATFORM_ADMIN", null, "Platform"
            );
        }

        Tenant tenant = tenantRepository.findById(principal.tenantId())
            .filter(candidate -> "ACTIVE".equals(candidate.getStatus()))
            .orElseThrow(() -> new TenantRoutingException("Tenant is inactive or unavailable"));
        TenantContext.set(tenant.getId());
        try {
            User user = userRepository.findById(principal.userId())
                .filter(User::isActive)
                .orElseThrow(() -> new InvalidCredentialsException("Authenticated account is inactive or no longer exists"));
            return toTenantResponse(user, tenant);
        } finally {
            TenantContext.clear();
        }
    }

    private AuthenticatedUserResponse toTenantResponse(User user, Tenant tenant) {
        return new AuthenticatedUserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            tenant.getBusinessType(),
            tenant.getName()
        );
    }
}
