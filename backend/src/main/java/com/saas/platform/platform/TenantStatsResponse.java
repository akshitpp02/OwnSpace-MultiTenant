package com.saas.platform.platform;

import java.util.Map;

public record TenantStatsResponse(
    long totalTenants,
    Map<String, Long> tenantsByType,
    Map<String, Long> tenantsByStatus
) {
}
