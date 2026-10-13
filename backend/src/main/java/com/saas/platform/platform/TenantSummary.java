package com.saas.platform.platform;

import java.time.LocalDateTime;

public record TenantSummary(
    Long id,
    String code,
    String name,
    String businessType,
    String status,
    LocalDateTime createdAt
) {
    public static TenantSummary from(Tenant tenant) {
        return new TenantSummary(
            tenant.getId(),
            tenant.getCode(),
            tenant.getName(),
            tenant.getBusinessType(),
            tenant.getStatus(),
            tenant.getCreatedAt()
        );
    }
}
