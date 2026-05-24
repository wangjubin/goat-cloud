package com.goat.cloud.module.system.controller;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.module.system.model.request.LoginRequest;
import com.goat.cloud.module.system.model.request.RefreshTokenRequest;
import com.goat.cloud.module.system.model.vo.LoginResponseVO;
import com.goat.cloud.module.system.model.vo.ProfileVO;
import com.goat.cloud.module.system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponseVO> login(@RequestBody @Valid LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.login(request, servletRequest));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponseVO> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success();
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileVO> profile() {
        return ApiResponse.success(authService.profile());
    }
}
