package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.procurement.ExpenseBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.ExpenseDto;
import com.hashcode.invoicely.entity.relational.*;
import com.hashcode.invoicely.repository.relational.*;
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
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final VendorRepository vendorRepository;
    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;
    private final VendorService vendorService;
    private final ClientService clientService;
    private final ExpenseCategoryService expenseCategoryService;
    private final InvoiceService invoiceService;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Transactional(readOnly = true)
    public GenericManyResponse<ExpenseDto> getExpenses(int page, int perPage, String status, UUID vendorId, UUID clientId, String filter, String sort) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 100;

        Sort sorting = Sort.by(Sort.Direction.DESC, "updatedAt");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split("\\|");
            if (parts.length == 2) {
                sorting = Sort.by("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);

        Specification<ExpenseEntity> spec = (root, query, cb) -> {
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

            if (vendorId != null) {
                predicates.add(cb.equal(root.get("vendorId"), vendorId));
            }

            if (clientId != null) {
                predicates.add(cb.equal(root.get("clientId"), clientId));
            }

            if (StringUtils.hasText(filter)) {
                String pattern = "%" + filter.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("number")), pattern),
                        cb.like(cb.lower(root.get("publicNotes")), pattern),
                        cb.like(cb.lower(root.get("transactionReference")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ExpenseEntity> paged = expenseRepository.findAll(spec, pageable);
        List<ExpenseDto> dtos = paged.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return GenericManyResponse.of(dtos, paged.getTotalElements(), paged.getTotalPages(), pageSize);
    }

    @Transactional(readOnly = true)
    public ExpenseDto getBlankExpense() {
        UUID companyId = getRequiredCompanyId();
        String nextNumber = sequenceGeneratorService.generateSequence("expense_" + companyId);

        return ExpenseDto.builder()
                .number(nextNumber)
                .amount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .foreignAmount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .exchangeRate(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP))
                .currencyId("USD")
                .date(LocalDate.now().toString())
                .paymentTypeId("1")
                .taxRate1(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxRate2(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxRate3(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxAmount1(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxAmount2(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxAmount3(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .usesInclusiveTaxes(false)
                .shouldBeInvoiced(false)
                .statusId("logged")
                .isDeleted(false)
                .documents(Collections.emptyList())
                .build();
    }

    @Transactional(readOnly = true)
    public ExpenseDto getExpenseById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        ExpenseEntity entity = expenseRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + id));
        return toDto(entity);
    }

    @Transactional
    public ExpenseDto createExpense(ExpenseDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        String number = StringUtils.hasText(dto.getNumber())
                ? dto.getNumber().trim()
                : sequenceGeneratorService.generateSequence("expense_" + companyId);

        LocalDate date = LocalDate.now();
        if (StringUtils.hasText(dto.getDate())) {
            try {
                date = LocalDate.parse(dto.getDate().trim());
            } catch (DateTimeParseException ignored) {}
        }

        LocalDate paymentDate = null;
        if (StringUtils.hasText(dto.getPaymentDate())) {
            try {
                paymentDate = LocalDate.parse(dto.getPaymentDate().trim());
            } catch (DateTimeParseException ignored) {}
        }

        BigDecimal amount = dto.getAmount() != null ? dto.getAmount().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal foreignAmount = dto.getForeignAmount() != null ? dto.getForeignAmount().setScale(4, RoundingMode.HALF_UP) : amount;
        BigDecimal exchangeRate = dto.getExchangeRate() != null ? dto.getExchangeRate().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);

        BigDecimal r1 = dto.getTaxRate1() != null ? dto.getTaxRate1().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal r2 = dto.getTaxRate2() != null ? dto.getTaxRate2().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal r3 = dto.getTaxRate3() != null ? dto.getTaxRate3().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        BigDecimal taxAmount1 = calculateTaxAmount(amount, r1, dto.isUsesInclusiveTaxes());
        BigDecimal taxAmount2 = calculateTaxAmount(amount, r2, dto.isUsesInclusiveTaxes());
        BigDecimal taxAmount3 = calculateTaxAmount(amount, r3, dto.isUsesInclusiveTaxes());

        ExpenseEntity entity = ExpenseEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .assignedUserId(dto.getAssignedUserId())
                .vendorId(dto.getVendorId())
                .clientId(dto.getClientId())
                .invoiceId(dto.getInvoiceId())
                .categoryId(dto.getCategoryId())
                .purchaseOrderId(dto.getPurchaseOrderId())
                .number(number)
                .amount(amount)
                .foreignAmount(foreignAmount)
                .exchangeRate(exchangeRate)
                .currencyId(StringUtils.hasText(dto.getCurrencyId()) ? dto.getCurrencyId() : "USD")
                .taxName1(dto.getTaxName1())
                .taxRate1(r1)
                .taxName2(dto.getTaxName2())
                .taxRate2(r2)
                .taxName3(dto.getTaxName3())
                .taxRate3(r3)
                .taxAmount1(taxAmount1)
                .taxAmount2(taxAmount2)
                .taxAmount3(taxAmount3)
                .usesInclusiveTaxes(dto.isUsesInclusiveTaxes())
                .shouldBeInvoiced(dto.isShouldBeInvoiced())
                .date(date)
                .paymentDate(paymentDate)
                .paymentTypeId(StringUtils.hasText(dto.getPaymentTypeId()) ? dto.getPaymentTypeId() : "1")
                .transactionReference(dto.getTransactionReference())
                .publicNotes(dto.getPublicNotes())
                .privateNotes(dto.getPrivateNotes())
                .customValue1(dto.getCustomValue1())
                .customValue2(dto.getCustomValue2())
                .customValue3(dto.getCustomValue3())
                .customValue4(dto.getCustomValue4())
                .statusId(StringUtils.hasText(dto.getStatusId()) ? dto.getStatusId() : "logged")
                .isDeleted(false)
                .build();

        ExpenseEntity saved = expenseRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public ExpenseDto updateExpense(UUID id, ExpenseDto dto) {
        UUID companyId = getRequiredCompanyId();
        ExpenseEntity entity = expenseRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + id));

        if (dto.getVendorId() != null) entity.setVendorId(dto.getVendorId());
        if (dto.getClientId() != null) entity.setClientId(dto.getClientId());
        if (dto.getCategoryId() != null) entity.setCategoryId(dto.getCategoryId());
        if (dto.getAssignedUserId() != null) entity.setAssignedUserId(dto.getAssignedUserId());
        if (StringUtils.hasText(dto.getNumber())) entity.setNumber(dto.getNumber().trim());

        if (StringUtils.hasText(dto.getDate())) {
            try {
                entity.setDate(LocalDate.parse(dto.getDate().trim()));
            } catch (DateTimeParseException ignored) {}
        }
        if (StringUtils.hasText(dto.getPaymentDate())) {
            try {
                entity.setPaymentDate(LocalDate.parse(dto.getPaymentDate().trim()));
            } catch (DateTimeParseException ignored) {}
        }

        if (dto.getAmount() != null) entity.setAmount(dto.getAmount().setScale(4, RoundingMode.HALF_UP));
        if (dto.getForeignAmount() != null) entity.setForeignAmount(dto.getForeignAmount().setScale(4, RoundingMode.HALF_UP));
        if (dto.getExchangeRate() != null) entity.setExchangeRate(dto.getExchangeRate().setScale(4, RoundingMode.HALF_UP));
        if (dto.getCurrencyId() != null) entity.setCurrencyId(dto.getCurrencyId());

        if (dto.getTaxName1() != null) entity.setTaxName1(dto.getTaxName1());
        if (dto.getTaxRate1() != null) entity.setTaxRate1(dto.getTaxRate1().setScale(4, RoundingMode.HALF_UP));
        if (dto.getTaxName2() != null) entity.setTaxName2(dto.getTaxName2());
        if (dto.getTaxRate2() != null) entity.setTaxRate2(dto.getTaxRate2().setScale(4, RoundingMode.HALF_UP));
        if (dto.getTaxName3() != null) entity.setTaxName3(dto.getTaxName3());
        if (dto.getTaxRate3() != null) entity.setTaxRate3(dto.getTaxRate3().setScale(4, RoundingMode.HALF_UP));

        entity.setUsesInclusiveTaxes(dto.isUsesInclusiveTaxes());
        entity.setShouldBeInvoiced(dto.isShouldBeInvoiced());

        entity.setTaxAmount1(calculateTaxAmount(entity.getAmount(), entity.getTaxRate1(), entity.isUsesInclusiveTaxes()));
        entity.setTaxAmount2(calculateTaxAmount(entity.getAmount(), entity.getTaxRate2(), entity.isUsesInclusiveTaxes()));
        entity.setTaxAmount3(calculateTaxAmount(entity.getAmount(), entity.getTaxRate3(), entity.isUsesInclusiveTaxes()));

        if (dto.getPaymentTypeId() != null) entity.setPaymentTypeId(dto.getPaymentTypeId());
        if (dto.getTransactionReference() != null) entity.setTransactionReference(dto.getTransactionReference());
        if (dto.getPublicNotes() != null) entity.setPublicNotes(dto.getPublicNotes());
        if (dto.getPrivateNotes() != null) entity.setPrivateNotes(dto.getPrivateNotes());
        if (dto.getStatusId() != null) entity.setStatusId(dto.getStatusId());

        if (dto.getCustomValue1() != null) entity.setCustomValue1(dto.getCustomValue1());
        if (dto.getCustomValue2() != null) entity.setCustomValue2(dto.getCustomValue2());
        if (dto.getCustomValue3() != null) entity.setCustomValue3(dto.getCustomValue3());
        if (dto.getCustomValue4() != null) entity.setCustomValue4(dto.getCustomValue4());

        ExpenseEntity updated = expenseRepository.save(entity);
        return toDto(updated);
    }

    @Transactional
    public void deleteExpense(UUID id) {
        UUID companyId = getRequiredCompanyId();
        ExpenseEntity entity = expenseRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + id));
        entity.setDeleted(true);
        expenseRepository.save(entity);
    }

    @Transactional
    public List<ExpenseDto> bulkAction(ExpenseBulkActionRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return List.of();
        }
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();
        List<ExpenseEntity> entities = expenseRepository.findByCompanyIdAndIdIn(companyId, request.getIds());

        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";
        switch (action) {
            case "archive" -> {
                for (ExpenseEntity entity : entities) {
                    entity.setArchivedAt(Instant.now());
                }
            }
            case "restore" -> {
                for (ExpenseEntity entity : entities) {
                    entity.setArchivedAt(null);
                    entity.setDeleted(false);
                }
            }
            case "delete" -> {
                for (ExpenseEntity entity : entities) {
                    entity.setDeleted(true);
                }
            }
            case "bulk_categorize" -> {
                UUID categoryId = request.getCategoryId();
                for (ExpenseEntity entity : entities) {
                    entity.setCategoryId(categoryId);
                }
            }
            case "convert_to_invoice" -> {
                UUID determinedClientId = request.getClientId() != null
                        ? request.getClientId()
                        : entities.stream()
                                .map(ExpenseEntity::getClientId)
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("A client ID is required to invoice expenses"));

                ClientEntity client = clientRepository.findByCompanyIdAndId(companyId, determinedClientId)
                        .orElseThrow(() -> new IllegalArgumentException("Client not found: " + determinedClientId));

                InvoiceEntity invoice;
                if (request.getInvoiceId() != null) {
                    invoice = invoiceRepository.findByCompanyIdAndId(companyId, request.getInvoiceId())
                            .orElseThrow(() -> new IllegalArgumentException("Target invoice not found: " + request.getInvoiceId()));
                } else {
                    String invNumber = sequenceGeneratorService.generateSequence("invoice_" + companyId);
                    invoice = InvoiceEntity.builder()
                            .companyId(companyId)
                            .clientId(client.getId())
                            .userId(userId)
                            .number(invNumber)
                            .date(LocalDate.now())
                            .dueDate(LocalDate.now().plusDays(30))
                            .statusId("draft")
                            .amount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                            .balance(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                            .paidToDate(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                            .isDeleted(false)
                            .build();
                }

                BigDecimal addedAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
                int order = invoice.getLineItems().size();

                for (ExpenseEntity exp : entities) {
                    InvoiceItemEntity item = InvoiceItemEntity.builder()
                            .productKey("EXPENSE")
                            .notes("Expense #" + exp.getNumber() + (StringUtils.hasText(exp.getPublicNotes()) ? ": " + exp.getPublicNotes() : ""))
                            .cost(exp.getAmount())
                            .quantity(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP))
                            .taxName1(exp.getTaxName1())
                            .taxRate1(exp.getTaxRate1())
                            .taxName2(exp.getTaxName2())
                            .taxRate2(exp.getTaxRate2())
                            .taxName3(exp.getTaxName3())
                            .taxRate3(exp.getTaxRate3())
                            .lineTotal(exp.getAmount())
                            .sortId(order++)
                            .build();

                    invoice.addLineItem(item);
                    addedAmount = addedAmount.add(exp.getAmount());

                    exp.setInvoiceId(invoice.getId());
                    exp.setStatusId("invoiced");
                }

                invoice.setAmount(invoice.getAmount().add(addedAmount));
                invoice.setBalance(invoice.getBalance().add(addedAmount));
                InvoiceEntity savedInvoice = invoiceRepository.save(invoice);

                client.setBalance(client.getBalance().add(addedAmount));
                clientRepository.save(client);

                for (ExpenseEntity exp : entities) {
                    exp.setInvoiceId(savedInvoice.getId());
                }
            }
            default -> throw new IllegalArgumentException("Unsupported bulk action: " + action);
        }

        expenseRepository.saveAll(entities);
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    private BigDecimal calculateTaxAmount(BigDecimal amount, BigDecimal rate, boolean isInclusive) {
        if (amount == null || rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        if (isInclusive) {
            BigDecimal hundred = new BigDecimal("100");
            BigDecimal divisor = hundred.add(rate);
            return amount.multiply(rate).divide(divisor, 4, RoundingMode.HALF_UP);
        } else {
            return amount.multiply(rate).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        }
    }

    public ExpenseDto toDto(ExpenseEntity entity) {
        if (entity == null) return null;
        return ExpenseDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .vendorId(entity.getVendorId())
                .clientId(entity.getClientId())
                .invoiceId(entity.getInvoiceId())
                .categoryId(entity.getCategoryId())
                .purchaseOrderId(entity.getPurchaseOrderId())
                .number(entity.getNumber())
                .amount(entity.getAmount())
                .foreignAmount(entity.getForeignAmount())
                .exchangeRate(entity.getExchangeRate())
                .currencyId(entity.getCurrencyId())
                .taxName1(entity.getTaxName1())
                .taxRate1(entity.getTaxRate1())
                .taxName2(entity.getTaxName2())
                .taxRate2(entity.getTaxRate2())
                .taxName3(entity.getTaxName3())
                .taxRate3(entity.getTaxRate3())
                .taxAmount1(entity.getTaxAmount1())
                .taxAmount2(entity.getTaxAmount2())
                .taxAmount3(entity.getTaxAmount3())
                .usesInclusiveTaxes(entity.isUsesInclusiveTaxes())
                .shouldBeInvoiced(entity.isShouldBeInvoiced())
                .date(entity.getDate() != null ? entity.getDate().toString() : null)
                .paymentDate(entity.getPaymentDate() != null ? entity.getPaymentDate().toString() : null)
                .paymentTypeId(entity.getPaymentTypeId())
                .transactionReference(entity.getTransactionReference())
                .publicNotes(entity.getPublicNotes())
                .privateNotes(entity.getPrivateNotes())
                .customValue1(entity.getCustomValue1())
                .customValue2(entity.getCustomValue2())
                .customValue3(entity.getCustomValue3())
                .customValue4(entity.getCustomValue4())
                .statusId(entity.getStatusId())
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .vendor(entity.getVendorId() != null ? vendorRepository.findById(entity.getVendorId()).map(vendorService::toDto).orElse(null) : null)
                .client(entity.getClientId() != null ? clientRepository.findById(entity.getClientId()).map(clientService::toDto).orElse(null) : null)
                .category(entity.getCategoryId() != null ? expenseCategoryRepository.findById(entity.getCategoryId()).map(expenseCategoryService::toDto).orElse(null) : null)
                .invoice(entity.getInvoiceId() != null ? invoiceRepository.findById(entity.getInvoiceId()).map(invoiceService::toDto).orElse(null) : null)
                .documents(Collections.emptyList())
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
