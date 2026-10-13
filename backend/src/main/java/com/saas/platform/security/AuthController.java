package com.saas.platform.security;

import com.saas.platform.common.ApiResponse;
import com.saas.platform.core.ChangePasswordRequest;
import com.saas.platform.core.CoreService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CoreService coreService;

    public AuthController(AuthService authService, CoreService coreService) {
        this.authService = authService;
        this.coreService = coreService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody TenantLoginRequest request) {
        return ApiResponse.success("Login successful", authService.loginTenant(request));
    }

    @PostMapping("/platform-login")
    public ApiResponse<LoginResponse> platformLogin(@Valid @RequestBody PlatformLoginRequest request) {
        return ApiResponse.success("Login successful", authService.loginPlatformAdmin(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthenticatedUserResponse> me(Authentication authentication) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success("Current user retrieved", authService.currentUser(principal));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        Authentication authentication) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        if (principal.tenantId() == null) {
            throw new IllegalArgumentException("Platform admin password change is not supported here");
        }
        coreService.changePassword(principal.userId(), request.currentPassword(), request.newPassword());
        return ApiResponse.success("Password changed successfully", null);
    }
}
