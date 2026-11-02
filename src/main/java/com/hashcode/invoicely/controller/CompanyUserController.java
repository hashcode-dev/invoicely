package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.auth.CompanyUserDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.service.CompanyUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/company_users")
@RequiredArgsConstructor
public class CompanyUserController {

    private final CompanyUserService companyUserService;

    @GetMapping("/{userId}/preferences")
    public ResponseEntity<Map<String, Object>> getPreferences(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(Map.of("data", companyUserService.getPreferences(userId)));
    }

    @PutMapping("/{userId}/preferences")
    public ResponseEntity<Map<String, Object>> updatePreferences(
            @PathVariable("userId") UUID userId,
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(Map.of("data", companyUserService.updatePreferences(userId, payload)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyUserDto>> updateCompanyUser(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(ApiResponse.of(companyUserService.updateCompanyUser(id, payload)));
    }
}
