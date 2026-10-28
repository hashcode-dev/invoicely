package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.InvoiceDto;
import com.hashcode.invoicely.dto.invoice.QuoteBulkActionRequest;
import com.hashcode.invoicely.dto.invoice.QuoteDto;
import com.hashcode.invoicely.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<QuoteDto>> getQuotes(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "20") int perPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "client_id", required = false) UUID clientId,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(quoteService.getQuotes(page, perPage, status, clientId, filter, sort));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<QuoteDto>> getBlankQuote(
            @RequestParam(value = "client_id", required = false) UUID clientId) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.getBlankQuote(clientId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuoteDto>> getQuoteById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.getQuoteById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QuoteDto>> createQuote(@RequestBody QuoteDto quoteDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(quoteService.createQuote(quoteDto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuoteDto>> updateQuote(
            @PathVariable("id") UUID id,
            @RequestBody QuoteDto quoteDto) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.updateQuote(id, quoteDto)));
    }

    @PostMapping("/{id}/convert_to_invoice")
    public ResponseEntity<ApiResponse<InvoiceDto>> convertToInvoice(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.convertToInvoice(id)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<QuoteDto>>> bulkAction(@RequestBody QuoteBulkActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.bulkAction(request)));
    }
}
