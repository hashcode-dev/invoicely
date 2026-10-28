package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.payment.CompanyGatewayBulkActionRequest;
import com.hashcode.invoicely.dto.payment.CompanyGatewayDto;
import com.hashcode.invoicely.service.CompanyGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/company_gateways")
@RequiredArgsConstructor
public class CompanyGatewayController {

    private final CompanyGatewayService companyGatewayService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<CompanyGatewayDto>> getCompanyGateways(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "100") int perPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(companyGatewayService.getCompanyGateways(page, perPage, status, filter, sort));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<CompanyGatewayDto>> getBlankCompanyGateway() {
        return ResponseEntity.ok(ApiResponse.of(companyGatewayService.getBlankCompanyGateway()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyGatewayDto>> getCompanyGatewayById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(companyGatewayService.getCompanyGatewayById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyGatewayDto>> createCompanyGateway(@RequestBody CompanyGatewayDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(companyGatewayService.createCompanyGateway(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyGatewayDto>> updateCompanyGateway(
            @PathVariable("id") UUID id,
            @RequestBody CompanyGatewayDto dto) {
        return ResponseEntity.ok(ApiResponse.of(companyGatewayService.updateCompanyGateway(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCompanyGateway(@PathVariable("id") UUID id) {
        companyGatewayService.deleteCompanyGateway(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<CompanyGatewayDto>>> bulkAction(@RequestBody CompanyGatewayBulkActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(companyGatewayService.bulkAction(request)));
    }
}
