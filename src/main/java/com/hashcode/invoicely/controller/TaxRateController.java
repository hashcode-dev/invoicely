package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.TaxRateDto;
import com.hashcode.invoicely.service.TaxRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tax_rates")
@RequiredArgsConstructor
public class TaxRateController {

    private final TaxRateService taxRateService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<TaxRateDto>> getTaxRates() {
        return ResponseEntity.ok(taxRateService.getTaxRates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaxRateDto>> getTaxRateById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(taxRateService.getTaxRateById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaxRateDto>> createTaxRate(@RequestBody TaxRateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(taxRateService.createTaxRate(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaxRateDto>> updateTaxRate(
            @PathVariable("id") UUID id,
            @RequestBody TaxRateDto dto) {
        return ResponseEntity.ok(ApiResponse.of(taxRateService.updateTaxRate(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTaxRate(@PathVariable("id") UUID id) {
        taxRateService.deleteTaxRate(id);
        return ResponseEntity.noContent().build();
    }
}
