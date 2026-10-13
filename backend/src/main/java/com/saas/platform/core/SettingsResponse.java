package com.saas.platform.core;

import java.math.BigDecimal;

public record SettingsResponse(String businessName, String phone, String address, BigDecimal taxPercent) {
}
