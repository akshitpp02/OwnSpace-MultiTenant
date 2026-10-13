package com.saas.platform.platform;

import com.saas.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
public class TenantProvisioningController {

    private final TenantProvisioningService tenantProvisioningService;

    public TenantProvisioningController(TenantProvisioningService tenantProvisioningService) {
        this.tenantProvisioningService = tenantProvisioningService;
    }

    @PostMapping("/tenants/register")
    public ResponseEntity<ApiResponse<TenantRegistrationResponse>> register(@Valid @RequestBody TenantRegistrationRequest request) {
        ApiResponse<TenantRegistrationResponse> response = tenantProvisioningService.register(request);
        return ResponseEntity.ok(response);
    }
}
