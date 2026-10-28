package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.auth.CompanyDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.company.CreateCompanyRequest;
import com.hashcode.invoicely.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<CompanyDto>> getCompanies(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "20") int perPage) {
        return ResponseEntity.ok(companyService.getCompanies(page, perPage));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyDto>> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.createCompany(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyDto>> getCompany(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(companyService.getCompany(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyDto>> updateCompany(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateCompanyRequest request) {
        return ResponseEntity.ok(companyService.updateCompany(id, request));
    }
}
