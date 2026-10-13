package com.saas.platform.core;

import com.saas.platform.common.ApiResponse;
import com.saas.platform.common.PageResponse;
import com.saas.platform.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class UserController {

    private final CoreService coreService;

    public UserController(CoreService coreService) {
        this.coreService = coreService;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Users retrieved", coreService.listUsers(search, page, size));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(
        @Valid @RequestBody UserRequest request,
        Authentication authentication) {
        return ApiResponse.success(
            "User created",
            coreService.createUser(request, (AuthUserPrincipal) authentication.getPrincipal())
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(
        @PathVariable @Min(1) long id,
        @Valid @RequestBody UserRequest request,
        Authentication authentication) {
        return ApiResponse.success(
            "User updated",
            coreService.updateUser(id, request, (AuthUserPrincipal) authentication.getPrincipal())
        );
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<UserResponse> setActive(
        @PathVariable @Min(1) long id,
        @Valid @RequestBody ActiveStatusRequest request,
        Authentication authentication) {
        return ApiResponse.success(
            "User status updated",
            coreService.setUserActive(id, request.active(), (AuthUserPrincipal) authentication.getPrincipal())
        );
    }
}
