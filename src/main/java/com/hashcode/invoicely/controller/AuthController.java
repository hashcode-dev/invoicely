package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.auth.CompanyUserResponse;
import com.hashcode.invoicely.dto.auth.LoginPrecheckRequest;
import com.hashcode.invoicely.dto.auth.LoginPrecheckResponse;
import com.hashcode.invoicely.dto.auth.LoginRequest;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login/precheck")
    public ResponseEntity<LoginPrecheckResponse> precheck(@Valid @RequestBody LoginPrecheckRequest request) {
        return ResponseEntity.ok(authService.precheck(request));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<List<CompanyUserResponse>>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<List<CompanyUserResponse>>> register(
            @Valid @RequestBody com.hashcode.invoicely.dto.auth.RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping(path = {"/auth/google", "/oauth_login"})
    public ResponseEntity<ApiResponse<List<CompanyUserResponse>>> loginWithGoogle(
            @RequestBody com.hashcode.invoicely.dto.auth.GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    @PostMapping(path = {"/auth/employee_login", "/login/employee"})
    public ResponseEntity<ApiResponse<List<CompanyUserResponse>>> employeeLogin(
            @Valid @RequestBody com.hashcode.invoicely.dto.auth.EmployeeLoginRequest request) {
        return ResponseEntity.ok(authService.employeeLogin(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<List<CompanyUserResponse>>> refresh(
            @RequestParam(value = "current_company", required = false) UUID currentCompanyId) {
        return ResponseEntity.ok(authService.refresh(currentCompanyId));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "logged_out"));
    }
}
