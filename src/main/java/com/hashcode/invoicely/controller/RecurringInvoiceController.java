package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.RecurringInvoiceDto;
import com.hashcode.invoicely.service.RecurringInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recurring_invoices")
@RequiredArgsConstructor
public class RecurringInvoiceController {

    private final RecurringInvoiceService recurringInvoiceService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<RecurringInvoiceDto>> getRecurringInvoices(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "20") int perPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "filter", required = false) String filter) {
        return ResponseEntity.ok(recurringInvoiceService.getRecurringInvoices(page, perPage, status, filter));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<RecurringInvoiceDto>> getBlankRecurringInvoice() {
        return ResponseEntity.ok(ApiResponse.of(recurringInvoiceService.getBlankRecurringInvoice()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringInvoiceDto>> getRecurringInvoiceById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(recurringInvoiceService.getRecurringInvoiceById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecurringInvoiceDto>> createRecurringInvoice(@RequestBody RecurringInvoiceDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(recurringInvoiceService.createRecurringInvoice(dto)));
    }
}
