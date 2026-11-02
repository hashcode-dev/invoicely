package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.navigation.NavigationConfigDto;
import com.hashcode.invoicely.service.NavigationConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/navigation")
@RequiredArgsConstructor
public class NavigationConfigController {

    private final NavigationConfigService navigationConfigService;

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getResolvedConfig(
            @RequestParam(value = "company_id", required = false) UUID companyId) {
        UUID effectiveCompanyId = companyId != null ? companyId : TenantContext.getCompanyId();
        Map<String, NavigationConfigDto> config =
                navigationConfigService.getResolvedNavigationConfig(effectiveCompanyId, TenantContext.getUserId());
        return ResponseEntity.ok(Map.of("data", config));
    }

    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<NavigationConfigDto>>> getRules(
            @RequestParam(value = "role", defaultValue = "DEFAULT") String role,
            @RequestParam(value = "company_id", required = false) UUID companyId) {
        UUID effectiveCompanyId = companyId != null ? companyId : TenantContext.getCompanyId();
        List<NavigationConfigDto> rules = navigationConfigService.getCompanyNavigationRules(effectiveCompanyId, role);
        return ResponseEntity.ok(ApiResponse.of(rules));
    }

    @PutMapping("/rules")
    public ResponseEntity<ApiResponse<List<NavigationConfigDto>>> saveRules(
            @RequestParam(value = "role", defaultValue = "DEFAULT") String role,
            @RequestParam(value = "company_id", required = false) UUID companyId,
            @RequestBody List<NavigationConfigDto> items) {
        UUID effectiveCompanyId = companyId != null ? companyId : TenantContext.getCompanyId();
        List<NavigationConfigDto> saved = navigationConfigService.saveCompanyNavigationRules(effectiveCompanyId, role, items);
        return ResponseEntity.ok(ApiResponse.of(saved));
    }
}
