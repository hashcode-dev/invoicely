package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.InvoiceBulkActionRequest;
import com.hashcode.invoicely.dto.invoice.InvoiceDto;
import com.hashcode.invoicely.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<InvoiceDto>> getInvoices(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "20") int perPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "client_id", required = false) UUID clientId,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(invoiceService.getInvoices(page, perPage, status, clientId, filter, sort));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<InvoiceDto>> getBlankInvoice(
            @RequestParam(value = "client_id", required = false) UUID clientId) {
        return ResponseEntity.ok(ApiResponse.of(invoiceService.getBlankInvoice(clientId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoiceById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(invoiceService.getInvoiceById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceDto>> createInvoice(@RequestBody InvoiceDto invoiceDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(invoiceService.createInvoice(invoiceDto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> updateInvoice(
            @PathVariable("id") UUID id,
            @RequestBody InvoiceDto invoiceDto) {
        return ResponseEntity.ok(ApiResponse.of(invoiceService.updateInvoice(id, invoiceDto)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> bulkAction(@RequestBody InvoiceBulkActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(invoiceService.bulkAction(request)));
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<ApiResponse<InvoiceDto>> cloneInvoice(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(invoiceService.cloneInvoice(id)));
    }

    @PostMapping("/{id}/mark_sent")
    public ResponseEntity<ApiResponse<InvoiceDto>> markSent(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(invoiceService.markSent(id)));
    }

    @PostMapping("/{id}/mark_paid")
    public ResponseEntity<ApiResponse<InvoiceDto>> markPaid(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(invoiceService.markPaid(id)));
    }
}
