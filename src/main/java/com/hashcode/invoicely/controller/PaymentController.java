package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.payment.CreatePaymentRequest;
import com.hashcode.invoicely.dto.payment.PaymentBulkActionRequest;
import com.hashcode.invoicely.dto.payment.PaymentDto;
import com.hashcode.invoicely.dto.payment.PaymentRefundRequest;
import com.hashcode.invoicely.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<PaymentDto>> getPayments(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "20") int perPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "client_id", required = false) UUID clientId,
            @RequestParam(value = "invoice_id", required = false) UUID invoiceId,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(paymentService.getPayments(page, perPage, status, clientId, invoiceId, filter, sort));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<PaymentDto>> getBlankPayment(
            @RequestParam(value = "client_id", required = false) UUID clientId) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.getBlankPayment(clientId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentDto>> getPaymentById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.getPaymentById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentDto>> createPayment(@RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(paymentService.createPayment(request)));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<PaymentDto>> refundPayment(
            @PathVariable("id") UUID id,
            @RequestBody PaymentRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.refundPayment(id, request)));
    }

    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<PaymentDto>> refundPaymentWithoutPathId(
            @RequestBody PaymentRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.refundPayment(request.getId(), request)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<PaymentDto>>> bulkAction(@RequestBody PaymentBulkActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.bulkAction(request)));
    }
}
