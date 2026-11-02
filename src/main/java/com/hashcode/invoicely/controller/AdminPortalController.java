package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.admin.AdminCustomerDto;
import com.hashcode.invoicely.dto.admin.AdminMetricsDto;
import com.hashcode.invoicely.dto.admin.SupportTicketDto;
import com.hashcode.invoicely.dto.admin.SupportTicketMessageDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.service.AdminPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminPortalController {

    private final AdminPortalService adminPortalService;

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<AdminMetricsDto>> getMetrics() {
        return ResponseEntity.ok(ApiResponse.of(adminPortalService.getPlatformMetrics()));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<List<AdminCustomerDto>>> getCustomers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "50") int perPage) {
        return ResponseEntity.ok(ApiResponse.of(adminPortalService.getAllCustomers(page, perPage)));
    }

    @GetMapping("/customers/{companyId}")
    public ResponseEntity<ApiResponse<AdminCustomerDto>> getCustomerDetails(
            @PathVariable("companyId") UUID companyId) {
        return ResponseEntity.ok(ApiResponse.of(adminPortalService.getCustomerDetails(companyId)));
    }

    @PostMapping("/customers/{companyId}/impersonate")
    public ResponseEntity<Map<String, String>> impersonateCustomer(
            @PathVariable("companyId") UUID companyId) {
        String token = adminPortalService.impersonateCustomer(companyId, TenantContext.getUserId());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/support/tickets")
    public ResponseEntity<ApiResponse<List<SupportTicketDto>>> getSupportTickets(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "30") int perPage) {
        return ResponseEntity.ok(ApiResponse.of(adminPortalService.getSupportTickets(status, page, perPage)));
    }

    @PostMapping("/support/tickets")
    public ResponseEntity<ApiResponse<SupportTicketDto>> createSupportTicket(
            @RequestBody Map<String, String> request) {
        UUID accountId = request.get("account_id") != null ? UUID.fromString(request.get("account_id")) : TenantContext.getAccountId();
        UUID companyId = request.get("company_id") != null ? UUID.fromString(request.get("company_id")) : TenantContext.getCompanyId();
        UUID reporterUserId = TenantContext.getUserId();

        SupportTicketDto ticket = adminPortalService.createSupportTicket(
                accountId,
                companyId,
                reporterUserId != null ? reporterUserId : UUID.randomUUID(),
                request.get("subject"),
                request.get("priority"),
                request.get("category"),
                request.get("message")
        );
        return ResponseEntity.ok(ApiResponse.of(ticket));
    }

    @GetMapping("/support/tickets/{ticketId}/messages")
    public ResponseEntity<ApiResponse<List<SupportTicketMessageDto>>> getTicketMessages(
            @PathVariable("ticketId") UUID ticketId) {
        return ResponseEntity.ok(ApiResponse.of(adminPortalService.getTicketMessages(ticketId)));
    }

    @PostMapping("/support/tickets/{ticketId}/reply")
    public ResponseEntity<ApiResponse<SupportTicketMessageDto>> replyToTicket(
            @PathVariable("ticketId") UUID ticketId,
            @RequestBody Map<String, Object> request) {
        UUID senderUserId = TenantContext.getUserId();
        String messageBody = (String) request.get("message");
        boolean isInternalNote = Boolean.TRUE.equals(request.get("is_internal_note"));
        String newStatus = (String) request.get("status");

        SupportTicketMessageDto message = adminPortalService.replyToTicket(
                ticketId,
                senderUserId != null ? senderUserId : UUID.randomUUID(),
                messageBody,
                isInternalNote,
                newStatus
        );
        return ResponseEntity.ok(ApiResponse.of(message));
    }
}
