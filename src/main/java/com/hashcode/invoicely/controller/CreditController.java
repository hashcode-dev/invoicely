package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.CreditDto;
import com.hashcode.invoicely.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<CreditDto>> getCredits(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "20") int perPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "client_id", required = false) UUID clientId,
            @RequestParam(value = "filter", required = false) String filter) {
        return ResponseEntity.ok(creditService.getCredits(page, perPage, status, clientId, filter));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<CreditDto>> getBlankCredit(
            @RequestParam(value = "client_id", required = false) UUID clientId) {
        return ResponseEntity.ok(ApiResponse.of(creditService.getBlankCredit(clientId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CreditDto>> getCreditById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(creditService.getCreditById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreditDto>> createCredit(@RequestBody CreditDto creditDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(creditService.createCredit(creditDto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CreditDto>> updateCredit(
            @PathVariable("id") UUID id,
            @RequestBody CreditDto creditDto) {
        return ResponseEntity.ok(ApiResponse.of(creditService.updateCredit(id, creditDto)));
    }
}
