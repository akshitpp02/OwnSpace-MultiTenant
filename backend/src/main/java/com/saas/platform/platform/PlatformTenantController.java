package com.saas.platform.platform;

import com.saas.platform.common.ApiResponse;
import com.saas.platform.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequestMapping("/api/platform")
public class PlatformTenantController {

    private final PlatformTenantService platformTenantService;

    public PlatformTenantController(PlatformTenantService platformTenantService) {
        this.platformTenantService = platformTenantService;
    }

    @GetMapping("/tenants")
    public ApiResponse<PageResponse<TenantSummary>> listTenants(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String type,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ApiResponse.success(
            "Tenants retrieved",
            platformTenantService.listTenants(status, type, page, size)
        );
    }

    @PatchMapping("/tenants/{id}/status")
    public ApiResponse<TenantSummary> changeStatus(
        @PathVariable @Min(1) long id,
        @Valid @RequestBody TenantStatusRequest request) {
        return ApiResponse.success(
            "Tenant status updated",
            platformTenantService.changeTenantStatus(id, request.status())
        );
    }

    @GetMapping("/stats")
    public ApiResponse<TenantStatsResponse> stats() {
        return ApiResponse.success("Platform stats retrieved", platformTenantService.getStats());
    }
}
