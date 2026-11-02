package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.config.security.JwtTokenProvider;
import com.hashcode.invoicely.dto.admin.AdminCustomerDto;
import com.hashcode.invoicely.dto.admin.AdminMetricsDto;
import com.hashcode.invoicely.dto.admin.SupportTicketDto;
import com.hashcode.invoicely.dto.admin.SupportTicketMessageDto;
import com.hashcode.invoicely.entity.relational.*;
import com.hashcode.invoicely.repository.relational.*;
import com.hashcode.invoicely.service.AdminPortalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPortalServiceImpl implements AdminPortalService {

    private final CompanyRepository companyRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CompanyUserRepository companyUserRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final SupportTicketMessageRepository supportTicketMessageRepository;
    private final PlatformAuditLogRepository platformAuditLogRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional(readOnly = true)
    public AdminMetricsDto getPlatformMetrics() {
        long totalTenants = companyRepository.count();
        long totalActiveUsers = userRepository.count();
        long totalInvoices = invoiceRepository.count();
        long openTickets = supportTicketRepository.countByStatus("OPEN");

        BigDecimal totalInvoicedAmount = invoiceRepository.findAll().stream()
                .map(InvoiceEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenueCollected = paymentRepository.findAll().stream()
                .map(PaymentEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminMetricsDto.builder()
                .totalTenants(totalTenants)
                .totalActiveUsers(totalActiveUsers)
                .totalInvoices(totalInvoices)
                .totalInvoicedAmount(totalInvoicedAmount)
                .totalRevenueCollected(totalRevenueCollected)
                .openTicketsCount(openTickets)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminCustomerDto> getAllCustomers(int page, int perPage) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, perPage));
        Page<CompanyEntity> companies = companyRepository.findAll(pageable);

        List<AdminCustomerDto> result = new ArrayList<>();
        for (CompanyEntity c : companies) {
            result.add(buildCustomerDto(c));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCustomerDto getCustomerDetails(UUID companyId) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
        return buildCustomerDto(company);
    }

    @Override
    @Transactional
    public String impersonateCustomer(UUID companyId, UUID adminUserId) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

        List<CompanyUserEntity> links = companyUserRepository.findByCompanyId(companyId);
        CompanyUserEntity targetUserLink = links.stream()
                .filter(CompanyUserEntity::isOwner)
                .findFirst()
                .orElseGet(() -> links.isEmpty() ? null : links.get(0));

        if (targetUserLink == null) {
            throw new IllegalStateException("No active users found for company: " + companyId);
        }

        UserEntity targetUser = userRepository.findById(targetUserLink.getUserId())
                .orElseThrow(() -> new IllegalStateException("Target user not found: " + targetUserLink.getUserId()));

        platformAuditLogRepository.save(PlatformAuditLogEntity.builder()
                .adminId(adminUserId != null ? adminUserId : targetUser.getId())
                .action("IMPERSONATE_TENANT")
                .targetAccountId(company.getAccountId())
                .targetCompanyId(company.getId())
                .details("Operator impersonated tenant owner: " + targetUser.getEmail())
                .build());

        return jwtTokenProvider.generateToken(targetUser.getId(), targetUser.getEmail(), company.getId(), company.getAccountId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketDto> getSupportTickets(String status, int page, int perPage) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, perPage));
        Page<SupportTicketEntity> pageResult;

        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            pageResult = supportTicketRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable);
        } else {
            pageResult = supportTicketRepository.findAll(pageable);
        }

        return pageResult.stream()
                .map(this::toTicketDto)
                .toList();
    }

    @Override
    @Transactional
    public SupportTicketDto createSupportTicket(
            UUID accountId, UUID companyId, UUID reporterUserId,
            String subject, String priority, String category, String initialMessage) {

        String ticketNumber = "TICK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        SupportTicketEntity ticket = SupportTicketEntity.builder()
                .ticketNumber(ticketNumber)
                .accountId(accountId)
                .companyId(companyId)
                .reporterUserId(reporterUserId)
                .subject(subject != null ? subject.trim() : "Support Request")
                .status("OPEN")
                .priority(priority != null ? priority.toUpperCase() : "MEDIUM")
                .category(category != null ? category.toUpperCase() : "GENERAL")
                .build();
        ticket = supportTicketRepository.save(ticket);

        if (initialMessage != null && !initialMessage.isBlank()) {
            supportTicketMessageRepository.save(SupportTicketMessageEntity.builder()
                    .ticketId(ticket.getId())
                    .senderUserId(reporterUserId)
                    .isInternalNote(false)
                    .messageBody(initialMessage.trim())
                    .build());
        }

        return toTicketDto(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketMessageDto> getTicketMessages(UUID ticketId) {
        List<SupportTicketMessageEntity> messages = supportTicketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        List<SupportTicketMessageDto> result = new ArrayList<>();

        for (SupportTicketMessageEntity m : messages) {
            String senderEmail = userRepository.findById(m.getSenderUserId())
                    .map(UserEntity::getEmail)
                    .orElse("Unknown");

            result.add(SupportTicketMessageDto.builder()
                    .id(m.getId())
                    .ticketId(m.getTicketId())
                    .senderUserId(m.getSenderUserId())
                    .senderEmail(senderEmail)
                    .isInternalNote(m.isInternalNote())
                    .messageBody(m.getMessageBody())
                    .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().getEpochSecond() : null)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional
    public SupportTicketMessageDto replyToTicket(
            UUID ticketId, UUID senderUserId, String messageBody, boolean isInternalNote, String newStatus) {

        SupportTicketEntity ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        SupportTicketMessageEntity message = SupportTicketMessageEntity.builder()
                .ticketId(ticketId)
                .senderUserId(senderUserId)
                .isInternalNote(isInternalNote)
                .messageBody(messageBody != null ? messageBody.trim() : "")
                .build();
        message = supportTicketMessageRepository.save(message);

        if (newStatus != null && !newStatus.isBlank()) {
            ticket.setStatus(newStatus.toUpperCase());
            if (newStatus.equalsIgnoreCase("RESOLVED") || newStatus.equalsIgnoreCase("CLOSED")) {
                ticket.setResolvedAt(Instant.now());
            }
            supportTicketRepository.save(ticket);
        }

        String senderEmail = userRepository.findById(senderUserId)
                .map(UserEntity::getEmail)
                .orElse("Operator");

        return SupportTicketMessageDto.builder()
                .id(message.getId())
                .ticketId(message.getTicketId())
                .senderUserId(message.getSenderUserId())
                .senderEmail(senderEmail)
                .isInternalNote(message.isInternalNote())
                .messageBody(message.getMessageBody())
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().getEpochSecond() : null)
                .build();
    }

    private AdminCustomerDto buildCustomerDto(CompanyEntity c) {
        String plan = "free";
        if (c.getAccountId() != null) {
            plan = accountRepository.findById(c.getAccountId())
                    .map(AccountEntity::getPlan)
                    .orElse("free");
        }

        List<CompanyUserEntity> userLinks = companyUserRepository.findByCompanyId(c.getId());
        String ownerEmail = "None";
        for (CompanyUserEntity l : userLinks) {
            if (l.isOwner()) {
                ownerEmail = userRepository.findById(l.getUserId())
                        .map(UserEntity::getEmail)
                        .orElse("None");
                break;
            }
        }
        if (ownerEmail.equals("None") && !userLinks.isEmpty()) {
            ownerEmail = userRepository.findById(userLinks.get(0).getUserId())
                    .map(UserEntity::getEmail)
                    .orElse("None");
        }

        long invoiceCount = invoiceRepository.findAll().stream()
                .filter(i -> c.getId().equals(i.getCompanyId()))
                .count();

        BigDecimal totalInvoiced = invoiceRepository.findAll().stream()
                .filter(i -> c.getId().equals(i.getCompanyId()))
                .map(InvoiceEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = paymentRepository.findAll().stream()
                .filter(p -> c.getId().equals(p.getCompanyId()))
                .map(PaymentEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminCustomerDto.builder()
                .companyId(c.getId())
                .accountId(c.getAccountId())
                .companyName(c.getCompanyName())
                .subdomain(c.getSubdomain())
                .plan(plan)
                .ownerEmail(ownerEmail)
                .usersCount(userLinks.size())
                .invoicesCount(invoiceCount)
                .totalInvoiced(totalInvoiced)
                .totalPaid(totalPaid)
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().getEpochSecond() : null)
                .build();
    }

    private SupportTicketDto toTicketDto(SupportTicketEntity t) {
        String companyName = "Unknown";
        if (t.getCompanyId() != null) {
            companyName = companyRepository.findById(t.getCompanyId())
                    .map(CompanyEntity::getCompanyName)
                    .orElse("Unknown");
        }

        String reporterEmail = userRepository.findById(t.getReporterUserId())
                .map(UserEntity::getEmail)
                .orElse("Unknown");

        return SupportTicketDto.builder()
                .id(t.getId())
                .ticketNumber(t.getTicketNumber())
                .accountId(t.getAccountId())
                .companyId(t.getCompanyId())
                .companyName(companyName)
                .reporterEmail(reporterEmail)
                .subject(t.getSubject())
                .status(t.getStatus())
                .priority(t.getPriority())
                .category(t.getCategory())
                .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().getEpochSecond() : null)
                .updatedAt(t.getUpdatedAt() != null ? t.getUpdatedAt().getEpochSecond() : null)
                .resolvedAt(t.getResolvedAt() != null ? t.getResolvedAt().getEpochSecond() : null)
                .build();
    }
}
