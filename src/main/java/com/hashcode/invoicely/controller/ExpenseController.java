package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.procurement.ExpenseBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.ExpenseDto;
import com.hashcode.invoicely.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<ExpenseDto>> getExpenses(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "100") int perPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "vendor_id", required = false) UUID vendorId,
            @RequestParam(value = "client_id", required = false) UUID clientId,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(expenseService.getExpenses(page, perPage, status, vendorId, clientId, filter, sort));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<ExpenseDto>> getBlankExpense() {
        return ResponseEntity.ok(ApiResponse.of(expenseService.getBlankExpense()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseDto>> getExpenseById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(expenseService.getExpenseById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseDto>> createExpense(@RequestBody ExpenseDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(expenseService.createExpense(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseDto>> updateExpense(
            @PathVariable("id") UUID id,
            @RequestBody ExpenseDto dto) {
        return ResponseEntity.ok(ApiResponse.of(expenseService.updateExpense(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable("id") UUID id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<ExpenseDto>>> bulkAction(@RequestBody ExpenseBulkActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(expenseService.bulkAction(request)));
    }
}
