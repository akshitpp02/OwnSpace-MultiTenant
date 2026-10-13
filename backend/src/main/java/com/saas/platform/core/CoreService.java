package com.saas.platform.core;

import com.saas.platform.common.ConflictException;
import com.saas.platform.common.PageResponse;
import com.saas.platform.common.ResourceNotFoundException;
import com.saas.platform.platform.Tenant;
import com.saas.platform.platform.TenantRepository;
import com.saas.platform.security.AuthUserPrincipal;
import com.saas.platform.tenancy.BusinessType;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoreService {

    private static final Set<String> RESTAURANT_ROLES = Set.of("MANAGER", "WAITER", "KITCHEN", "KITCHEN_STAFF", "CASHIER");
    private static final Set<String> RETAIL_ROLES = Set.of("MANAGER", "CASHIER", "STOCK_KEEPER");
    private static final Set<String> MANAGEMENT_ROLES = Set.of("OWNER", "MANAGER");
    private static final Map<String, Integer> SETTING_MAX_LENGTHS = Map.of(
        "business_name", 150,
        "phone", 20,
        "address", 250
    );

    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public CoreService(
        UserRepository userRepository,
        SettingsRepository settingsRepository,
        TenantRepository tenantRepository,
        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public PageResponse<UserResponse> listUsers(String search, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        String query = search == null ? "" : search.trim();
        Page<User> users = query.isEmpty()
            ? userRepository.findAll(pageable)
            : userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query, pageable);
        return PageResponse.from(users.map(UserResponse::from));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public UserResponse createUser(UserRequest request, AuthUserPrincipal actor) {
        String role = normalizeRole(request.role());
        requireAllowedRole(role, actor.businessType());
        requirePrivilegedRolePermission(actor, role, null);

        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required when creating a user");
        }
        validatePassword(request.password());

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("A user with this email already exists in this business");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setActive(true);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public UserResponse updateUser(long id, UserRequest request, AuthUserPrincipal actor) {
        User target = findUser(id);
        String role = normalizeRole(request.role());
        requireAllowedRole(role, actor.businessType());
        requirePrivilegedRolePermission(actor, role, target);
        if (actor.userId().equals(target.getId()) && "OWNER".equals(target.getRole()) && !"OWNER".equals(role)) {
            throw new AccessDeniedException("An owner cannot demote their own account");
        }

        String email = normalizeEmail(request.email());
        userRepository.findByEmail(email)
            .filter(existing -> !existing.getId().equals(target.getId()))
            .ifPresent(existing -> {
                throw new ConflictException("A user with this email already exists in this business");
            });

        target.setName(request.name().trim());
        target.setEmail(email);
        target.setRole(role);
        if (request.password() != null && !request.password().isBlank()) {
            validatePassword(request.password());
            target.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(userRepository.save(target));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public UserResponse setUserActive(long id, boolean active, AuthUserPrincipal actor) {
        User target = findUser(id);
        if ("OWNER".equals(target.getRole()) && !"OWNER".equals(actor.role())) {
            throw new AccessDeniedException("Only an owner can change an owner's active status");
        }
        requirePrivilegedRolePermission(actor, target.getRole(), target);
        if (actor.userId().equals(target.getId()) && "OWNER".equals(target.getRole()) && !active) {
            throw new AccessDeniedException("An owner cannot deactivate their own account");
        }
        target.setActive(active);
        return UserResponse.from(userRepository.save(target));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public SettingsResponse getSettings(AuthUserPrincipal actor) {
        Map<String, String> values = readSettings();
        String businessName = values.get("business_name");
        if (businessName == null) {
            businessName = tenantRepository.findById(actor.tenantId())
                .map(Tenant::getName)
                .orElse("");
        }
        return toSettingsResponse(values, businessName);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public SettingsResponse updateSettings(SettingsRequest request, AuthUserPrincipal actor) {
        String businessName = request.businessName().trim();
        saveSetting("business_name", businessName);
        tenantRepository.findById(actor.tenantId()).ifPresent(tenant -> {
            tenant.setName(businessName);
            tenantRepository.save(tenant);
        });
        saveSetting("phone", normalizeOptional(request.phone()));
        saveSetting("address", normalizeOptional(request.address()));
        if (request.taxPercent() != null) {
            saveSetting("tax_percent", request.taxPercent().stripTrailingZeros().toPlainString());
        }
        return getSettings(actor);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public CoreDashboardResponse getDashboard(AuthUserPrincipal actor) {
        Map<String, String> values = readSettings();
        String businessName = values.get("business_name");
        if (businessName == null) {
            businessName = tenantRepository.findById(actor.tenantId())
                .map(Tenant::getName)
                .orElse("");
        }
        return new CoreDashboardResponse(userRepository.count(), userRepository.countByActiveTrue(), businessName);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void changePassword(long userId, String currentPassword, String newPassword) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private User findUser(long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void requireAllowedRole(String role, String businessType) {
        Set<String> allowedRoles = switch (BusinessType.valueOf(businessType)) {
            case RESTAURANT -> RESTAURANT_ROLES;
            case RETAIL -> RETAIL_ROLES;
        };
        if (!allowedRoles.contains(role)) {
            throw new IllegalArgumentException("Role " + role + " is not allowed for " + businessType + " businesses");
        }
    }

    private void requirePrivilegedRolePermission(AuthUserPrincipal actor, String requestedRole, User target) {
        boolean privilegedAccount = MANAGEMENT_ROLES.contains(requestedRole)
            || (target != null && MANAGEMENT_ROLES.contains(target.getRole()));
        if (privilegedAccount && !"OWNER".equals(actor.role())) {
            throw new AccessDeniedException("Only an owner can create or change owner and manager accounts");
        }
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePassword(String password) {
        if (password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("Password must be between 8 and 72 characters");
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<String, String> readSettings() {
        Map<String, String> values = new HashMap<>();
        for (Settings setting : settingsRepository.findAll()) {
            values.put(setting.getKey(), setting.getValue());
        }
        return values;
    }

    private void saveSetting(String key, String value) {
        Integer maxLength = SETTING_MAX_LENGTHS.get(key);
        if (maxLength != null && value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(key.replace('_', ' ') + " cannot exceed " + maxLength + " characters");
        }
        Settings setting = settingsRepository.findByKey(key).orElseGet(Settings::new);
        setting.setKey(key);
        setting.setValue(value);
        settingsRepository.save(setting);
    }

    private SettingsResponse toSettingsResponse(Map<String, String> values, String businessName) {
        BigDecimal taxPercent = values.containsKey("tax_percent") && values.get("tax_percent") != null
            ? new BigDecimal(values.get("tax_percent"))
            : null;
        return new SettingsResponse(
            businessName,
            values.get("phone"),
            values.get("address"),
            taxPercent
        );
    }
}
