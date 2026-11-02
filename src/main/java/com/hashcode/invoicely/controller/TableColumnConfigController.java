package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.table.TableColumnConfigDto;
import com.hashcode.invoicely.service.TableColumnConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class TableColumnConfigController {

    private final TableColumnConfigService tableColumnConfigService;

    @GetMapping("/{companyId}/table-columns")
    public ResponseEntity<ApiResponse<List<TableColumnConfigDto>>> getAllColumnConfigs(
            @PathVariable("companyId") UUID companyId) {
        UUID effectiveCompanyId = companyId != null ? companyId : TenantContext.getCompanyId();
        List<TableColumnConfigDto> configs = tableColumnConfigService.getAllTableColumnConfigs(effectiveCompanyId);
        return ResponseEntity.ok(ApiResponse.of(configs));
    }

    @GetMapping("/{companyId}/table-columns/{tableName}")
    public ResponseEntity<ApiResponse<TableColumnConfigDto>> getColumnConfig(
            @PathVariable("companyId") UUID companyId,
            @PathVariable("tableName") String tableName) {
        UUID effectiveCompanyId = companyId != null ? companyId : TenantContext.getCompanyId();
        TableColumnConfigDto config = tableColumnConfigService.getTableColumnConfig(effectiveCompanyId, tableName);
        return ResponseEntity.ok(ApiResponse.of(config));
    }

    @PutMapping("/{companyId}/table-columns/{tableName}")
    public ResponseEntity<ApiResponse<TableColumnConfigDto>> saveColumnConfig(
            @PathVariable("companyId") UUID companyId,
            @PathVariable("tableName") String tableName,
            @RequestBody TableColumnConfigDto request) {
        UUID effectiveCompanyId = companyId != null ? companyId : TenantContext.getCompanyId();
        TableColumnConfigDto updated = tableColumnConfigService.saveTableColumnConfig(effectiveCompanyId, tableName, request);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }
}
