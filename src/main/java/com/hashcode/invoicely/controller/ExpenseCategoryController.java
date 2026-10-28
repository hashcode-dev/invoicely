package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.procurement.ExpenseCategoryBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.ExpenseCategoryDto;
import com.hashcode.invoicely.service.ExpenseCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expense_categories")
@RequiredArgsConstructor
public class ExpenseCategoryController {

    private final ExpenseCategoryService expenseCategoryService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<ExpenseCategoryDto>> getExpenseCategories(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "100") int perPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(expenseCategoryService.getExpenseCategories(page, perPage, status, filter, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseCategoryDto>> getExpenseCategoryById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(expenseCategoryService.getExpenseCategoryById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseCategoryDto>> createExpenseCategory(@RequestBody ExpenseCategoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(expenseCategoryService.createExpenseCategory(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseCategoryDto>> updateExpenseCategory(
            @PathVariable("id") UUID id,
            @RequestBody ExpenseCategoryDto dto) {
        return ResponseEntity.ok(ApiResponse.of(expenseCategoryService.updateExpenseCategory(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpenseCategory(@PathVariable("id") UUID id) {
        expenseCategoryService.deleteExpenseCategory(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<ExpenseCategoryDto>>> bulkAction(@RequestBody ExpenseCategoryBulkActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(expenseCategoryService.bulkAction(request)));
    }
}
