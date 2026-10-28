package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.banking.BankTransactionBulkActionRequest;
import com.hashcode.invoicely.dto.banking.BankTransactionDto;
import com.hashcode.invoicely.dto.banking.BankTransactionMatchRequest;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.service.BankTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/bank_transactions")
@RequiredArgsConstructor
public class BankTransactionController {

    private final BankTransactionService transactionService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<BankTransactionDto>> getTransactions(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "50") int perPage,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "bank_account_id", required = false) UUID bankAccountId,
            @RequestParam(name = "bank_integration_id", required = false) UUID bankIntegrationId,
            @RequestParam(name = "base_type", required = false) String baseType,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "sort", required = false) String sort) {
        UUID integrationId = bankIntegrationId != null ? bankIntegrationId : bankAccountId;
        return ResponseEntity.ok(transactionService.getTransactions(page, perPage, status, integrationId, baseType, filter, sort));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<BankTransactionDto>> getBlankTransaction() {
        return ResponseEntity.ok(ApiResponse.of(transactionService.getBlankTransaction()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BankTransactionDto>> getTransactionById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(transactionService.getTransactionById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BankTransactionDto>> createTransaction(@RequestBody BankTransactionDto dto) {
        BankTransactionDto created = transactionService.createTransaction(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BankTransactionDto>> updateTransaction(
            @PathVariable UUID id,
            @RequestBody BankTransactionDto dto) {
        BankTransactionDto updated = transactionService.updateTransaction(id, dto);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable UUID id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> handleBulkAction(@RequestBody BankTransactionBulkActionRequest request) {
        Map<String, Object> result = transactionService.handleBulkAction(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/match")
    public ResponseEntity<Map<String, Object>> matchTransactions(@RequestBody BankTransactionMatchRequest request) {
        Map<String, Object> result = transactionService.matchTransactions(request);
        return ResponseEntity.ok(result);
    }
}
