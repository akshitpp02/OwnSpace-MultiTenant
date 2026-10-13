package com.saas.platform.platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TenantRegistrationRequest {

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 150, message = "Business name must be between 2 and 150 characters")
    private String businessName;

    @NotBlank(message = "Business code is required")
    @Pattern(regexp = "^[a-z0-9-]{3,40}$", message = "Business code must be 3 to 40 chars, lowercase letters, numbers, or hyphens")
    private String businessCode;

    @NotBlank(message = "Business type is required")
    private String businessType;

    @NotBlank(message = "Owner name is required")
    @Size(min = 2, max = 120, message = "Owner name must be between 2 and 120 characters")
    private String ownerName;

    @NotBlank(message = "Owner email is required")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Owner email is invalid")
    private String ownerEmail;

    @NotBlank(message = "Owner password is required")
    @Size(min = 8, message = "Owner password must be at least 8 characters")
    private String ownerPassword;

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getBusinessCode() { return businessCode; }
    public void setBusinessCode(String businessCode) { this.businessCode = businessCode; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    public String getOwnerPassword() { return ownerPassword; }
    public void setOwnerPassword(String ownerPassword) { this.ownerPassword = ownerPassword; }
}
