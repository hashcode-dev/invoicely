package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.admin.AdminCustomerDto;
import com.hashcode.invoicely.dto.admin.AdminMetricsDto;
import com.hashcode.invoicely.dto.admin.SupportTicketDto;
import com.hashcode.invoicely.dto.admin.SupportTicketMessageDto;

import java.util.List;
import java.util.UUID;

public interface AdminPortalService {
    AdminMetricsDto getPlatformMetrics();
    List<AdminCustomerDto> getAllCustomers(int page, int perPage);
    AdminCustomerDto getCustomerDetails(UUID companyId);
    String impersonateCustomer(UUID companyId, UUID adminUserId);
    List<SupportTicketDto> getSupportTickets(String status, int page, int perPage);
    SupportTicketDto createSupportTicket(UUID accountId, UUID companyId, UUID reporterUserId, String subject, String priority, String category, String initialMessage);
    List<SupportTicketMessageDto> getTicketMessages(UUID ticketId);
    SupportTicketMessageDto replyToTicket(UUID ticketId, UUID senderUserId, String messageBody, boolean isInternalNote, String newStatus);
}
