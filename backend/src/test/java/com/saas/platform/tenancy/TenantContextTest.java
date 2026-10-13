package com.saas.platform.tenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void storesAndClearsTenantId() {
        TenantContext.set(17L);

        assertEquals(17L, TenantContext.get());

        TenantContext.clear();
        assertNull(TenantContext.get());
    }
}
