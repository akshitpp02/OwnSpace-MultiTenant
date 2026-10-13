package com.saas.platform.platform;

public class TenantRegistrationResponse {
    private final Long id;
    private final String code;
    private final String type;

    public TenantRegistrationResponse(Long id, String code, String type) {
        this.id = id;
        this.code = code;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }
}
