package com.saas.platform.core;

import com.saas.platform.common.ApiResponse;
import com.saas.platform.security.AuthUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class CoreDashboardController {

    private final CoreService coreService;

    public CoreDashboardController(CoreService coreService) {
        this.coreService = coreService;
    }

    @GetMapping("/core")
    public ApiResponse<CoreDashboardResponse> dashboard(Authentication authentication) {
        return ApiResponse.success(
            "Dashboard retrieved",
            coreService.getDashboard((AuthUserPrincipal) authentication.getPrincipal())
        );
    }
}
