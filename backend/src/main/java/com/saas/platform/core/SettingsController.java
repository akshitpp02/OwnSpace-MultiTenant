package com.saas.platform.core;

import com.saas.platform.common.ApiResponse;
import com.saas.platform.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final CoreService coreService;

    public SettingsController(CoreService coreService) {
        this.coreService = coreService;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<SettingsResponse> get(Authentication authentication) {
        return ApiResponse.success(
            "Settings retrieved",
            coreService.getSettings((AuthUserPrincipal) authentication.getPrincipal())
        );
    }

    @GetMapping("/tax")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER', 'STOCK_KEEPER')")
    public ApiResponse<TaxSettingsResponse> getTax(Authentication authentication) {
        BigDecimal taxPercent = coreService.getSettings(
            (AuthUserPrincipal) authentication.getPrincipal()
        ).taxPercent();
        return ApiResponse.success("Tax settings retrieved", new TaxSettingsResponse(taxPercent));
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<SettingsResponse> update(
        @Valid @RequestBody SettingsRequest request,
        Authentication authentication) {
        return ApiResponse.success(
            "Settings updated",
            coreService.updateSettings(request, (AuthUserPrincipal) authentication.getPrincipal())
        );
    }
}
