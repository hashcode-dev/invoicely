package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.banking.BankTransactionRuleBulkActionRequest;
import com.hashcode.invoicely.dto.banking.BankTransactionRuleDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.service.BankTransactionRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/bank_transaction_rules")
@RequiredArgsConstructor
public class BankTransactionRuleController {

    private final BankTransactionRuleService ruleService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<BankTransactionRuleDto>> getRules(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "50") int perPage,
            @RequestParam(name = "status", defaultValue = "active") String status,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "sort", required = false) String sort) {
        return ResponseEntity.ok(ruleService.getRules(page, perPage, status, filter, sort));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<BankTransactionRuleDto>> getBlankRule() {
        return ResponseEntity.ok(ApiResponse.of(ruleService.getBlankRule()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BankTransactionRuleDto>> getRuleById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(ruleService.getRuleById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BankTransactionRuleDto>> createRule(@RequestBody BankTransactionRuleDto dto) {
        BankTransactionRuleDto created = ruleService.createRule(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BankTransactionRuleDto>> updateRule(
            @PathVariable UUID id,
            @RequestBody BankTransactionRuleDto dto) {
        BankTransactionRuleDto updated = ruleService.updateRule(id, dto);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable UUID id) {
        ruleService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> handleBulkAction(@RequestBody BankTransactionRuleBulkActionRequest request) {
        int processed = ruleService.handleBulkAction(request);
        return ResponseEntity.ok(Map.of("processed", processed));
    }
}
