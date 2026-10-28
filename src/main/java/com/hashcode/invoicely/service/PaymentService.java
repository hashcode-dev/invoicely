package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.InvoiceDto;
import com.hashcode.invoicely.dto.payment.*;
import com.hashcode.invoicely.entity.relational.ClientEntity;
import com.hashcode.invoicely.entity.relational.InvoiceEntity;
import com.hashcode.invoicely.entity.relational.PaymentEntity;
import com.hashcode.invoicely.entity.relational.PaymentableEntity;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.InvoiceRepository;
import com.hashcode.invoicely.repository.relational.PaymentRepository;
import com.hashcode.invoicely.repository.relational.PaymentableRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentableRepository paymentableRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final ClientService clientService;
    private final InvoiceService invoiceService;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Transactional(readOnly = true)
    public GenericManyResponse<PaymentDto> getPayments(int page, int perPage, String status, UUID clientId, UUID invoiceId, String filter, String sort) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 20;

        Sort sorting = Sort.by(Sort.Direction.DESC, "updatedAt");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split("\\|");
            if (parts.length == 2) {
                sorting = Sort.by("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);

        Specification<PaymentEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));

            if ("archived".equalsIgnoreCase(status)) {
                predicates.add(cb.isNotNull(root.get("archivedAt")));
            } else if ("deleted".equalsIgnoreCase(status)) {
                predicates.add(cb.isTrue(root.get("isDeleted")));
            } else if (!"all".equalsIgnoreCase(status)) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
                predicates.add(cb.isNull(root.get("archivedAt")));
            }

            if (clientId != null) {
                predicates.add(cb.equal(root.get("clientId"), clientId));
            }

            if (invoiceId != null) {
                Join<PaymentEntity, PaymentableEntity> paymentableJoin = root.join("paymentables", JoinType.INNER);
                predicates.add(cb.equal(paymentableJoin.get("invoiceId"), invoiceId));
                query.distinct(true);
            }

            if (StringUtils.hasText(filter)) {
                String pattern = "%" + filter.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("number")), pattern),
                        cb.like(cb.lower(root.get("transactionReference")), pattern),
                        cb.like(cb.lower(root.get("privateNotes")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<PaymentEntity> paged = paymentRepository.findAll(spec, pageable);
        List<PaymentDto> dtos = paged.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return GenericManyResponse.of(dtos, paged.getTotalElements(), paged.getTotalPages(), pageSize);
    }

    @Transactional(readOnly = true)
    public PaymentDto getBlankPayment(UUID clientId) {
        UUID companyId = getRequiredCompanyId();
        String nextNumber = sequenceGeneratorService.generateSequence("payment_" + companyId);

        return PaymentDto.builder()
                .number(nextNumber)
                .amount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .refunded(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .applied(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .date(LocalDate.now().toString())
                .typeId("1")
                .statusId("1")
                .isManual(true)
                .clientId(clientId)
                .isDeleted(false)
                .paymentables(new ArrayList<>())
                .invoices(new ArrayList<>())
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentDto getPaymentById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        PaymentEntity entity = paymentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
        return toDto(entity);
    }

    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        if (request.getClientId() == null) {
            throw new IllegalArgumentException("Client ID is required to create a payment");
        }

        ClientEntity client = clientRepository.findByCompanyIdAndId(companyId, request.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + request.getClientId()));

        String paymentNumber = StringUtils.hasText(request.getNumber())
                ? request.getNumber().trim()
                : sequenceGeneratorService.generateSequence("payment_" + companyId);

        LocalDate paymentDate = LocalDate.now();
        if (StringUtils.hasText(request.getDate())) {
            try {
                paymentDate = LocalDate.parse(request.getDate().trim());
            } catch (DateTimeParseException e) {
                log.warn("Invalid payment date format: {}, using today", request.getDate());
            }
        }

        // Aggregate allocations from either request.invoices or request.paymentables
        List<PaymentInvoiceAllocationRequest> allocations = new ArrayList<>();
        if (request.getInvoices() != null && !request.getInvoices().isEmpty()) {
            allocations.addAll(request.getInvoices());
        } else if (request.getPaymentables() != null && !request.getPaymentables().isEmpty()) {
            allocations.addAll(request.getPaymentables());
        }

        BigDecimal appliedAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        for (PaymentInvoiceAllocationRequest alloc : allocations) {
            if (alloc.getAmount() != null) {
                appliedAmount = appliedAmount.add(alloc.getAmount().setScale(4, RoundingMode.HALF_UP));
            }
        }

        BigDecimal paymentAmount;
        if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            paymentAmount = request.getAmount().setScale(4, RoundingMode.HALF_UP);
        } else {
            paymentAmount = appliedAmount;
        }

        PaymentEntity payment = PaymentEntity.builder()
                .companyId(companyId)
                .clientId(client.getId())
                .userId(userId)
                .assignedUserId(request.getAssignedUserId())
                .companyGatewayId(request.getCompanyGatewayId())
                .number(paymentNumber)
                .amount(paymentAmount)
                .refunded(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .applied(appliedAmount)
                .date(paymentDate)
                .typeId(StringUtils.hasText(request.getTypeId()) ? request.getTypeId().trim() : "1")
                .transactionReference(request.getTransactionReference())
                .privateNotes(request.getPrivateNotes())
                .statusId(StringUtils.hasText(request.getStatusId()) ? request.getStatusId().trim() : "completed")
                .isManual(request.isManual())
                .customValue1(request.getCustomValue1())
                .customValue2(request.getCustomValue2())
                .customValue3(request.getCustomValue3())
                .customValue4(request.getCustomValue4())
                .isDeleted(false)
                .build();

        // Process invoice allocations
        for (PaymentInvoiceAllocationRequest alloc : allocations) {
            if (alloc.getInvoiceId() == null || alloc.getAmount() == null || alloc.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            InvoiceEntity invoice = invoiceRepository.findByCompanyIdAndId(companyId, alloc.getInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + alloc.getInvoiceId()));

            BigDecimal allocAmount = alloc.getAmount().setScale(4, RoundingMode.HALF_UP);

            // Update invoice balance and paidToDate
            BigDecimal newBalance = invoice.getBalance().subtract(allocAmount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                newBalance = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            }
            invoice.setBalance(newBalance);
            invoice.setPaidToDate(invoice.getPaidToDate().add(allocAmount));

            if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
                invoice.setStatusId("paid");
            } else {
                invoice.setStatusId("partial");
            }

            invoiceRepository.save(invoice);

            PaymentableEntity paymentable = PaymentableEntity.builder()
                    .invoiceId(invoice.getId())
                    .amount(allocAmount)
                    .refunded(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                    .build();

            payment.addPaymentable(paymentable);
        }

        // Client ledger updates
        BigDecimal newClientBalance = client.getBalance().subtract(appliedAmount);
        if (newClientBalance.compareTo(BigDecimal.ZERO) < 0) {
            newClientBalance = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        client.setBalance(newClientBalance);
        client.setPaidToDate(client.getPaidToDate().add(paymentAmount));

        // Excess payment credited to client
        if (paymentAmount.compareTo(appliedAmount) > 0) {
            BigDecimal unallocatedCredit = paymentAmount.subtract(appliedAmount);
            client.setCreditBalance(client.getCreditBalance().add(unallocatedCredit));
            log.info("Credited excess payment {} to client {} credit balance", unallocatedCredit, client.getId());
        }

        clientRepository.save(client);
        PaymentEntity savedPayment = paymentRepository.save(payment);
        return toDto(savedPayment);
    }

    @Transactional
    public PaymentDto refundPayment(UUID id, PaymentRefundRequest request) {
        UUID companyId = getRequiredCompanyId();

        UUID paymentId = id != null ? id : request.getId();
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID is required for refund");
        }

        PaymentEntity payment = paymentRepository.findByCompanyIdAndId(companyId, paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        ClientEntity client = clientRepository.findByCompanyIdAndId(companyId, payment.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + payment.getClientId()));

        BigDecimal maxRefundable = payment.getAmount().subtract(payment.getRefunded());

        // Determine total refund requested
        BigDecimal totalRefundRequested = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        if (request.getInvoices() != null && !request.getInvoices().isEmpty()) {
            for (PaymentInvoiceAllocationRequest alloc : request.getInvoices()) {
                if (alloc.getAmount() != null) {
                    totalRefundRequested = totalRefundRequested.add(alloc.getAmount().setScale(4, RoundingMode.HALF_UP));
                }
            }
        } else if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            totalRefundRequested = request.getAmount().setScale(4, RoundingMode.HALF_UP);
        } else {
            throw new IllegalArgumentException("Refund amount or refundable invoices are required");
        }

        if (totalRefundRequested.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }

        if (totalRefundRequested.compareTo(maxRefundable) > 0) {
            throw new IllegalArgumentException(String.format("Refund amount (%s) exceeds refundable balance (%s)", totalRefundRequested, maxRefundable));
        }

        // Process refund against specific invoices if provided
        if (request.getInvoices() != null && !request.getInvoices().isEmpty()) {
            for (PaymentInvoiceAllocationRequest alloc : request.getInvoices()) {
                if (alloc.getInvoiceId() == null || alloc.getAmount() == null || alloc.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                PaymentableEntity paymentable = payment.getPaymentables().stream()
                        .filter(p -> p.getInvoiceId().equals(alloc.getInvoiceId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Invoice " + alloc.getInvoiceId() + " not associated with payment " + payment.getId()));

                BigDecimal invoiceRefund = alloc.getAmount().setScale(4, RoundingMode.HALF_UP);
                paymentable.setRefunded(paymentable.getRefunded().add(invoiceRefund));

                InvoiceEntity invoice = invoiceRepository.findByCompanyIdAndId(companyId, alloc.getInvoiceId())
                        .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + alloc.getInvoiceId()));

                invoice.setBalance(invoice.getBalance().add(invoiceRefund));
                BigDecimal newPaid = invoice.getPaidToDate().subtract(invoiceRefund);
                invoice.setPaidToDate(newPaid.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : newPaid);

                if (invoice.getBalance().compareTo(invoice.getAmount()) >= 0) {
                    invoice.setStatusId("sent");
                } else {
                    invoice.setStatusId("partial");
                }
                invoiceRepository.save(invoice);

                // Restore client balance
                client.setBalance(client.getBalance().add(invoiceRefund));
                BigDecimal newClientPaid = client.getPaidToDate().subtract(invoiceRefund);
                client.setPaidToDate(newClientPaid.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : newClientPaid);
            }
        } else {
            // General amount refund across existing paymentables
            BigDecimal remainingToRefund = totalRefundRequested;
            for (PaymentableEntity paymentable : payment.getPaymentables()) {
                BigDecimal availableOnPaymentable = paymentable.getAmount().subtract(paymentable.getRefunded());
                if (availableOnPaymentable.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal refundForThis = remainingToRefund.min(availableOnPaymentable);
                    paymentable.setRefunded(paymentable.getRefunded().add(refundForThis));

                    InvoiceEntity invoice = invoiceRepository.findByCompanyIdAndId(companyId, paymentable.getInvoiceId()).orElse(null);
                    if (invoice != null) {
                        invoice.setBalance(invoice.getBalance().add(refundForThis));
                        BigDecimal newPaid = invoice.getPaidToDate().subtract(refundForThis);
                        invoice.setPaidToDate(newPaid.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : newPaid);

                        if (invoice.getBalance().compareTo(invoice.getAmount()) >= 0) {
                            invoice.setStatusId("sent");
                        } else {
                            invoice.setStatusId("partial");
                        }
                        invoiceRepository.save(invoice);
                    }

                    client.setBalance(client.getBalance().add(refundForThis));
                    BigDecimal newClientPaid = client.getPaidToDate().subtract(refundForThis);
                    client.setPaidToDate(newClientPaid.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : newClientPaid);

                    remainingToRefund = remainingToRefund.subtract(refundForThis);
                    if (remainingToRefund.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                }
            }

            // If remaining refund came from excess credit rather than invoice allocations
            if (remainingToRefund.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal uncredit = remainingToRefund.min(client.getCreditBalance());
                client.setCreditBalance(client.getCreditBalance().subtract(uncredit));
                BigDecimal newClientPaid = client.getPaidToDate().subtract(remainingToRefund);
                client.setPaidToDate(newClientPaid.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : newClientPaid);
            }
        }

        payment.setRefunded(payment.getRefunded().add(totalRefundRequested));
        if (payment.getRefunded().compareTo(payment.getAmount()) >= 0) {
            payment.setStatusId("refunded");
        } else {
            payment.setStatusId("partially_refunded");
        }

        clientRepository.save(client);
        PaymentEntity updated = paymentRepository.save(payment);
        return toDto(updated);
    }

    @Transactional
    public List<PaymentDto> bulkAction(PaymentBulkActionRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return List.of();
        }
        UUID companyId = getRequiredCompanyId();
        List<PaymentEntity> entities = paymentRepository.findByCompanyIdAndIdIn(companyId, request.getIds());

        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";
        for (PaymentEntity entity : entities) {
            switch (action) {
                case "archive" -> {
                    entity.setArchivedAt(Instant.now());
                }
                case "restore" -> {
                    entity.setArchivedAt(null);
                    entity.setDeleted(false);
                }
                case "delete" -> {
                    entity.setDeleted(true);
                }
                default -> throw new IllegalArgumentException("Unsupported bulk action: " + action);
            }
        }
        paymentRepository.saveAll(entities);
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    public PaymentDto toDto(PaymentEntity entity) {
        if (entity == null) return null;

        List<PaymentableDto> paymentableDtos = entity.getPaymentables() != null
                ? entity.getPaymentables().stream().map(this::toPaymentableDto).collect(Collectors.toList())
                : Collections.emptyList();

        List<InvoiceDto> invoiceDtos = new ArrayList<>();
        if (entity.getPaymentables() != null) {
            for (PaymentableEntity p : entity.getPaymentables()) {
                invoiceRepository.findById(p.getInvoiceId()).ifPresent(inv -> invoiceDtos.add(invoiceService.toDto(inv)));
            }
        }

        return PaymentDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .clientId(entity.getClientId())
                .companyGatewayId(entity.getCompanyGatewayId())
                .number(entity.getNumber())
                .amount(entity.getAmount())
                .refunded(entity.getRefunded())
                .applied(entity.getApplied())
                .date(entity.getDate() != null ? entity.getDate().toString() : null)
                .typeId(entity.getTypeId())
                .transactionReference(entity.getTransactionReference())
                .privateNotes(entity.getPrivateNotes())
                .statusId(entity.getStatusId())
                .isManual(entity.isManual())
                .customValue1(entity.getCustomValue1())
                .customValue2(entity.getCustomValue2())
                .customValue3(entity.getCustomValue3())
                .customValue4(entity.getCustomValue4())
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .paymentables(paymentableDtos)
                .invoices(invoiceDtos)
                .client(clientRepository.findById(entity.getClientId()).map(clientService::toDto).orElse(null))
                .documents(Collections.emptyList())
                .build();
    }

    public PaymentableDto toPaymentableDto(PaymentableEntity entity) {
        if (entity == null) return null;
        String invNumber = invoiceRepository.findById(entity.getInvoiceId())
                .map(InvoiceEntity::getNumber)
                .orElse(null);

        return PaymentableDto.builder()
                .id(entity.getId())
                .paymentId(entity.getPayment() != null ? entity.getPayment().getId() : null)
                .invoiceId(entity.getInvoiceId())
                .invoiceNumber(invNumber)
                .amount(entity.getAmount())
                .refunded(entity.getRefunded())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .build();
    }

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("Tenant company ID is required in request context");
        }
        return companyId;
    }
}
