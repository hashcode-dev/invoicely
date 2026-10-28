package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.procurement.ProductBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.ProductDto;
import com.hashcode.invoicely.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<ProductDto>> getProducts(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "500") int perPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(productService.getProducts(page, perPage, status, filter, sort));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<ProductDto>> getBlankProduct() {
        return ResponseEntity.ok(ApiResponse.of(productService.getBlankProduct()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProductById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(productService.getProductById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(@RequestBody ProductDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(productService.createProduct(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable("id") UUID id,
            @RequestBody ProductDto dto) {
        return ResponseEntity.ok(ApiResponse.of(productService.updateProduct(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable("id") UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<ProductDto>>> bulkAction(@RequestBody ProductBulkActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(productService.bulkAction(request)));
    }
}
