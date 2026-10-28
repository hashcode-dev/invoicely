package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.procurement.PurchaseOrderBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.PurchaseOrderDto;
import com.hashcode.invoicely.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase_orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<PurchaseOrderDto>> getPurchaseOrders(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "50") int perPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "vendor_id", required = false) UUID vendorId,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrders(page, perPage, status, vendorId, filter, sort));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> getBlankPurchaseOrder(
            @RequestParam(value = "vendor_id", required = false) UUID vendorId) {
        return ResponseEntity.ok(ApiResponse.of(purchaseOrderService.getBlankPurchaseOrder(vendorId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> getPurchaseOrderById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseOrderService.getPurchaseOrderById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> createPurchaseOrder(@RequestBody PurchaseOrderDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(purchaseOrderService.createPurchaseOrder(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> updatePurchaseOrder(
            @PathVariable("id") UUID id,
            @RequestParam(value = "mark_sent", defaultValue = "false") boolean markSent,
            @RequestBody PurchaseOrderDto dto) {
        if (markSent) {
            return ResponseEntity.ok(ApiResponse.of(purchaseOrderService.markSent(id)));
        }
        return ResponseEntity.ok(ApiResponse.of(purchaseOrderService.updatePurchaseOrder(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePurchaseOrder(@PathVariable("id") UUID id) {
        purchaseOrderService.deletePurchaseOrder(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<PurchaseOrderDto>>> bulkAction(@RequestBody PurchaseOrderBulkActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(purchaseOrderService.bulkAction(request)));
    }
}
