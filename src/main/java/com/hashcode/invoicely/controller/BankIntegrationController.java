package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.banking.BankAccountDto;
import com.hashcode.invoicely.dto.banking.BankIntegrationBulkActionRequest;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.service.BankIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/bank_integrations")
@RequiredArgsConstructor
public class BankIntegrationController {

    private final BankIntegrationService bankIntegrationService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<BankAccountDto>> getBankIntegrations(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "50") int perPage,
            @RequestParam(name = "status", defaultValue = "active") String status,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "sort", required = false) String sort) {
        return ResponseEntity.ok(bankIntegrationService.getBankIntegrations(page, perPage, status, filter, sort));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<BankAccountDto>> getBlankBankIntegration() {
        return ResponseEntity.ok(ApiResponse.of(bankIntegrationService.getBlankBankIntegration()));
    }

    @GetMapping("/refresh_accounts")
    public ResponseEntity<ApiResponse<List<BankAccountDto>>> refreshAccounts() {
        return ResponseEntity.ok(ApiResponse.of(bankIntegrationService.refreshAccounts()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BankAccountDto>> getBankIntegrationById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(bankIntegrationService.getBankIntegrationById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BankAccountDto>> createBankIntegration(@RequestBody BankAccountDto dto) {
        BankAccountDto created = bankIntegrationService.createBankIntegration(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BankAccountDto>> updateBankIntegration(
            @PathVariable UUID id,
            @RequestBody BankAccountDto dto) {
        BankAccountDto updated = bankIntegrationService.updateBankIntegration(id, dto);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBankIntegration(@PathVariable UUID id) {
        bankIntegrationService.deleteBankIntegration(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> handleBulkAction(@RequestBody BankIntegrationBulkActionRequest request) {
        int processed = bankIntegrationService.handleBulkAction(request);
        return ResponseEntity.ok(Map.of("processed", processed));
    }
}
