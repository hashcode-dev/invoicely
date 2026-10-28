package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.vendor.VendorBulkActionRequest;
import com.hashcode.invoicely.dto.vendor.VendorDto;
import com.hashcode.invoicely.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<VendorDto>> getVendors(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "20") int perPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(vendorService.getVendors(page, perPage, status, filter, sort));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<VendorDto>> getBlankVendor() {
        return ResponseEntity.ok(ApiResponse.of(vendorService.getBlankVendor()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorDto>> getVendorById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(vendorService.getVendorById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VendorDto>> createVendor(@RequestBody VendorDto vendorDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(vendorService.createVendor(vendorDto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorDto>> updateVendor(
            @PathVariable("id") UUID id,
            @RequestBody VendorDto vendorDto) {
        return ResponseEntity.ok(ApiResponse.of(vendorService.updateVendor(id, vendorDto)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<VendorDto>>> bulkAction(@RequestBody VendorBulkActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(vendorService.bulkAction(request)));
    }

    @PostMapping("/{id}/purge")
    public ResponseEntity<ApiResponse<VendorDto>> purgeVendor(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(vendorService.purgeVendor(id)));
    }
}
