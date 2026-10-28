package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.banking.BankTransactionBulkActionRequest;
import com.hashcode.invoicely.dto.banking.BankTransactionDto;
import com.hashcode.invoicely.dto.banking.BankTransactionMatchItem;
import com.hashcode.invoicely.dto.banking.BankTransactionMatchRequest;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankTransactionService {

    private final BankTransactionRepository bankTransactionRepository;
    private final BankIntegrationRepository bankIntegrationRepository;
    private final BankTransactionRuleRepository ruleRepository;
    private final BankTransactionRuleService ruleService;
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Transactional(readOnly = true)
    public GenericManyResponse<BankTransactionDto> getTransactions(int page, int perPage, String status, UUID bankIntegrationId, String baseType, String filter, String sort) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 50;

        Sort sorting = Sort.by(Sort.Direction.DESC, "date");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split("\\|");
            if (parts.length == 2) {
                sorting = Sort.by("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);

        Specification<BankTransactionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));

            if (bankIntegrationId != null) {
                predicates.add(cb.equal(root.get("bankIntegrationId"), bankIntegrationId));
            }
            if (StringUtils.hasText(baseType)) {
                predicates.add(cb.equal(cb.lower(root.get("baseType")), baseType.toLowerCase()));
            }

            if ("archived".equalsIgnoreCase(status)) {
                predicates.add(cb.isNotNull(root.get("archivedAt")));
                predicates.add(cb.isFalse(root.get("isDeleted")));
            } else if ("deleted".equalsIgnoreCase(status)) {
                predicates.add(cb.isTrue(root.get("isDeleted")));
            } else if ("all".equalsIgnoreCase(status)) {
                // all
            } else if (StringUtils.hasText(status) && !"active".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(cb.lower(root.get("statusId")), status.toLowerCase()));
                predicates.add(cb.isNull(root.get("archivedAt")));
                predicates.add(cb.isFalse(root.get("isDeleted")));
            } else {
                predicates.add(cb.isFalse(root.get("isDeleted")));
                predicates.add(cb.isNull(root.get("archivedAt")));
            }

            if (StringUtils.hasText(filter)) {
                String pattern = "%" + filter.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("participant")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<BankTransactionEntity> pageResult = bankTransactionRepository.findAll(spec, pageable);
        List<BankTransactionDto> dtos = pageResult.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return GenericManyResponse.of(dtos, pageResult.getTotalElements(), pageResult.getTotalPages(), pageSize);
    }

    @Transactional(readOnly = true)
    public BankTransactionDto getTransactionById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        BankTransactionEntity entity = bankTransactionRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Bank transaction not found: " + id));
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public BankTransactionDto getBlankTransaction() {
        return BankTransactionDto.builder()
                .date(LocalDate.now())
                .amount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .currencyId("USD")
                .baseType("debit")
                .statusId("unmatched")
                .isDeleted(false)
                .build();
    }

    @Transactional
    public BankTransactionDto createTransaction(BankTransactionDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        BigDecimal amount = dto.getAmount() != null ? dto.getAmount().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        String baseType = StringUtils.hasText(dto.getBaseType()) ? dto.getBaseType().toLowerCase() : "debit";

        BankTransactionEntity entity = BankTransactionEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .bankIntegrationId(dto.getBankIntegrationId())
                .date(dto.getDate() != null ? dto.getDate() : LocalDate.now())
                .amount(amount)
                .currencyId(StringUtils.hasText(dto.getCurrencyId()) ? dto.getCurrencyId() : "USD")
                .description(dto.getDescription())
                .baseType(baseType)
                .statusId("unmatched")
                .participant(dto.getParticipant())
                .participantName(dto.getParticipantName())
                .accountType(dto.getAccountType())
                .categoryType(dto.getCategoryType())
                .categoryId(dto.getCategoryId() != null ? dto.getCategoryId() : dto.getInvoicelyCategoryId())
                .vendorId(dto.getVendorId())
                .isDeleted(false)
                .build();

        // Update bank account balance if associated
        if (dto.getBankIntegrationId() != null) {
            bankIntegrationRepository.findByCompanyIdAndId(companyId, dto.getBankIntegrationId()).ifPresent(bankAccount -> {
                if ("credit".equalsIgnoreCase(baseType)) {
                    bankAccount.setBalance(bankAccount.getBalance().add(amount));
                } else {
                    bankAccount.setBalance(bankAccount.getBalance().subtract(amount));
                }
                bankIntegrationRepository.save(bankAccount);
            });
        }

        // Apply matching rules
        applyRulesToTransaction(entity, companyId);

        BankTransactionEntity saved = bankTransactionRepository.save(entity);
        log.info("Created bank transaction {} for amount {} ({})", saved.getId(), saved.getAmount(), saved.getBaseType());
        return toDto(saved);
    }

    @Transactional
    public BankTransactionDto updateTransaction(UUID id, BankTransactionDto dto) {
        UUID companyId = getRequiredCompanyId();
        BankTransactionEntity entity = bankTransactionRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Bank transaction not found: " + id));

        if (dto.getDate() != null) {
            entity.setDate(dto.getDate());
        }
        if (dto.getAmount() != null) {
            entity.setAmount(dto.getAmount().setScale(4, RoundingMode.HALF_UP));
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getBaseType() != null) {
            entity.setBaseType(dto.getBaseType());
        }
        if (dto.getStatusId() != null) {
            entity.setStatusId(dto.getStatusId());
        }
        if (dto.getParticipant() != null) {
            entity.setParticipant(dto.getParticipant());
        }
        if (dto.getParticipantName() != null) {
            entity.setParticipantName(dto.getParticipantName());
        }
        if (dto.getVendorId() != null) {
            entity.setVendorId(dto.getVendorId());
        }
        if (dto.getCategoryId() != null) {
            entity.setCategoryId(dto.getCategoryId());
        } else if (dto.getInvoicelyCategoryId() != null) {
            entity.setCategoryId(dto.getInvoicelyCategoryId());
        }

        BankTransactionEntity saved = bankTransactionRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public void deleteTransaction(UUID id) {
        UUID companyId = getRequiredCompanyId();
        BankTransactionEntity entity = bankTransactionRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Bank transaction not found: " + id));
        entity.setIsDeleted(true);
        bankTransactionRepository.save(entity);
        log.info("Soft deleted bank transaction {} for company {}", id, companyId);
    }

    @Transactional
    public Map<String, Object> handleBulkAction(BankTransactionBulkActionRequest request) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            return Map.of("processed", 0);
        }

        List<BankTransactionEntity> entities = bankTransactionRepository.findByCompanyIdAndIdIn(companyId, request.getIds());
        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";

        switch (action) {
            case "archive" -> {
                for (BankTransactionEntity entity : entities) {
                    entity.setArchivedAt(Instant.now());
                    entity.setIsDeleted(false);
                }
                bankTransactionRepository.saveAll(entities);
                return Map.of("processed", entities.size());
            }
            case "restore" -> {
                for (BankTransactionEntity entity : entities) {
                    entity.setArchivedAt(null);
                    entity.setIsDeleted(false);
                }
                bankTransactionRepository.saveAll(entities);
                return Map.of("processed", entities.size());
            }
            case "delete" -> {
                for (BankTransactionEntity entity : entities) {
                    entity.setIsDeleted(true);
                }
                bankTransactionRepository.saveAll(entities);
                return Map.of("processed", entities.size());
            }
            case "convert_to_payment" -> {
                int converted = 0;
                ClientEntity fallbackClient = clientRepository.findByCompanyIdAndIsDeletedFalse(companyId).stream().findFirst().orElseGet(() ->
                        clientRepository.save(ClientEntity.builder()
                                .companyId(companyId)
                                .name("Bank Deposit Client")
                                .clientNumber(sequenceGeneratorService.generateSequence("client_" + companyId))
                                .build())
                );
                for (BankTransactionEntity tx : entities) {
                    if (!"converted".equalsIgnoreCase(tx.getStatusId()) && !"matched".equalsIgnoreCase(tx.getStatusId())) {
                        PaymentEntity payment = PaymentEntity.builder()
                                .companyId(companyId)
                                .userId(userId)
                                .clientId(fallbackClient.getId())
                                .number(sequenceGeneratorService.generateSequence("payment_" + companyId))
                                .amount(tx.getAmount())
                                .applied(BigDecimal.ZERO)
                                .refunded(BigDecimal.ZERO)
                                .date(tx.getDate())
                                .typeId("bank_transfer")
                                .transactionReference("Bank Tx " + tx.getId())
                                .isDeleted(false)
                                .build();
                        PaymentEntity savedPay = paymentRepository.save(payment);

                        tx.setPaymentId(savedPay.getId());
                        tx.setStatusId("converted");
                        converted++;
                    }
                }
                bankTransactionRepository.saveAll(entities);
                return Map.of("processed", converted);
            }
            case "convert_to_expense" -> {
                int converted = 0;
                for (BankTransactionEntity tx : entities) {
                    if (!"converted".equalsIgnoreCase(tx.getStatusId()) && !"matched".equalsIgnoreCase(tx.getStatusId())) {
                        ExpenseEntity expense = ExpenseEntity.builder()
                                .companyId(companyId)
                                .userId(userId)
                                .vendorId(tx.getVendorId())
                                .categoryId(tx.getCategoryId())
                                .number(sequenceGeneratorService.generateSequence("expense_" + companyId))
                                .amount(tx.getAmount())
                                .foreignAmount(tx.getAmount())
                                .exchangeRate(BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP))
                                .date(tx.getDate())
                                .publicNotes(tx.getDescription())
                                .isDeleted(false)
                                .build();
                        ExpenseEntity savedExp = expenseRepository.save(expense);

                        tx.setExpenseId(savedExp.getId());
                        tx.setStatusId("converted");
                        converted++;
                    }
                }
                bankTransactionRepository.saveAll(entities);
                return Map.of("processed", converted);
            }
            default -> {
                log.warn("Unknown bank transaction bulk action: {}", action);
                return Map.of("processed", 0);
            }
        }
    }

    @Transactional
    public Map<String, Object> matchTransactions(BankTransactionMatchRequest request) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();
        if (request == null || request.getTransactions() == null || request.getTransactions().isEmpty()) {
            return Map.of("processed", 0);
        }

        int processed = 0;
        for (BankTransactionMatchItem item : request.getTransactions()) {
            if (item.getId() == null) continue;

            Optional<BankTransactionEntity> optTx = bankTransactionRepository.findByCompanyIdAndId(companyId, item.getId());
            if (optTx.isEmpty()) continue;
            BankTransactionEntity tx = optTx.get();

            // 1. Match/convert to invoice(s)
            if (StringUtils.hasText(item.getInvoiceIds())) {
                String[] idStrings = item.getInvoiceIds().split(",");
                List<UUID> invoiceUuids = Arrays.stream(idStrings)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(UUID::fromString)
                        .collect(Collectors.toList());

                if (!invoiceUuids.isEmpty()) {
                    List<InvoiceEntity> invoices = invoiceRepository.findByCompanyIdAndIdIn(companyId, invoiceUuids);
                    if (!invoices.isEmpty()) {
                        InvoiceEntity firstInv = invoices.get(0);
                        ClientEntity client = clientRepository.findByCompanyIdAndId(companyId, firstInv.getClientId())
                                .orElse(null);

                        PaymentEntity payment = PaymentEntity.builder()
                                .companyId(companyId)
                                .userId(userId)
                                .clientId(firstInv.getClientId())
                                .number(sequenceGeneratorService.generateSequence("payment_" + companyId))
                                .amount(tx.getAmount())
                                .applied(tx.getAmount())
                                .refunded(BigDecimal.ZERO)
                                .date(tx.getDate())
                                .typeId("bank_transfer")
                                .transactionReference("Bank Match " + tx.getId())
                                .isDeleted(false)
                                .build();

                        // Allocate across invoice(s)
                        BigDecimal remaining = tx.getAmount();
                        for (InvoiceEntity inv : invoices) {
                            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                            BigDecimal payPortion = inv.getBalance().min(remaining);
                            PaymentableEntity paymentable = PaymentableEntity.builder()
                                    .payment(payment)
                                    .invoiceId(inv.getId())
                                    .amount(payPortion)
                                    .build();
                            payment.addPaymentable(paymentable);

                            inv.setBalance(inv.getBalance().subtract(payPortion));
                            inv.setPaidToDate(inv.getPaidToDate().add(payPortion));
                            if (inv.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
                                inv.setStatusId("paid");
                            }
                            remaining = remaining.subtract(payPortion);
                        }
                        invoiceRepository.saveAll(invoices);
                        PaymentEntity savedPay = paymentRepository.save(payment);

                        // Update client balance
                        if (client != null) {
                            client.setPaidToDate(client.getPaidToDate().add(tx.getAmount()));
                            client.setBalance(client.getBalance().subtract(tx.getAmount()));
                            clientRepository.save(client);
                        }

                        tx.setPaymentId(savedPay.getId());
                        tx.setInvoiceIds(item.getInvoiceIds());
                        tx.setStatusId("converted");
                        processed++;
                    }
                }
            } else if (StringUtils.hasText(item.getPaymentId())) {
                try {
                    UUID payId = UUID.fromString(item.getPaymentId().split(",")[0].trim());
                    tx.setPaymentId(payId);
                    tx.setStatusId("matched");
                    processed++;
                } catch (Exception e) {
                    log.warn("Invalid payment ID: {}", item.getPaymentId());
                }
            } else if (StringUtils.hasText(item.getExpenseId())) {
                try {
                    UUID expId = UUID.fromString(item.getExpenseId().split(",")[0].trim());
                    tx.setExpenseId(expId);
                    tx.setStatusId("matched");
                    processed++;
                } catch (Exception e) {
                    log.warn("Invalid expense ID: {}", item.getExpenseId());
                }
            } else if (StringUtils.hasText(item.getVendorId()) || StringUtils.hasText(item.getCategoryId()) || StringUtils.hasText(item.getInvoicelyCategoryId())) {
                UUID vendorUuid = null;
                UUID categoryUuid = null;

                if (StringUtils.hasText(item.getVendorId())) {
                    try {
                        vendorUuid = UUID.fromString(item.getVendorId().split(",")[0].trim());
                    } catch (Exception ignored) {}
                }

                String rawCat = StringUtils.hasText(item.getCategoryId()) ? item.getCategoryId() : item.getInvoicelyCategoryId();
                if (StringUtils.hasText(rawCat)) {
                    try {
                        categoryUuid = UUID.fromString(rawCat.split(",")[0].trim());
                    } catch (Exception ignored) {}
                }

                ExpenseEntity expense = ExpenseEntity.builder()
                        .companyId(companyId)
                        .userId(userId)
                        .vendorId(vendorUuid)
                        .categoryId(categoryUuid)
                        .number(sequenceGeneratorService.generateSequence("expense_" + companyId))
                        .amount(tx.getAmount())
                        .foreignAmount(tx.getAmount())
                        .exchangeRate(BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP))
                        .date(tx.getDate())
                        .publicNotes(tx.getDescription())
                        .isDeleted(false)
                        .build();

                ExpenseEntity savedExp = expenseRepository.save(expense);
                tx.setExpenseId(savedExp.getId());
                tx.setVendorId(vendorUuid);
                tx.setCategoryId(categoryUuid);
                tx.setStatusId("converted");
                processed++;
            }

            bankTransactionRepository.save(tx);
        }

        log.info("Matched/converted {} bank transactions for company {}", processed, companyId);
        return Map.of("processed", processed, "success", true);
    }

    private void applyRulesToTransaction(BankTransactionEntity entity, UUID companyId) {
        List<BankTransactionRuleEntity> activeRules = ruleRepository.findByCompanyIdAndIsDeletedFalse(companyId);
        for (BankTransactionRuleEntity rule : activeRules) {
            if (ruleService.matches(rule, entity)) {
                entity.setBankTransactionRuleId(rule.getId());
                if (rule.getVendorId() != null) {
                    entity.setVendorId(rule.getVendorId());
                }
                if (rule.getCategoryId() != null) {
                    entity.setCategoryId(rule.getCategoryId());
                }
                if (Boolean.TRUE.equals(rule.getAutoConvert())) {
                    entity.setStatusId("converted");
                    if ("debit".equalsIgnoreCase(entity.getBaseType()) && (entity.getVendorId() != null || entity.getCategoryId() != null)) {
                        ExpenseEntity expense = ExpenseEntity.builder()
                                .companyId(companyId)
                                .userId(entity.getUserId())
                                .vendorId(entity.getVendorId())
                                .categoryId(entity.getCategoryId())
                                .number(sequenceGeneratorService.generateSequence("expense_" + companyId))
                                .amount(entity.getAmount())
                                .foreignAmount(entity.getAmount())
                                .exchangeRate(BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP))
                                .date(entity.getDate())
                                .publicNotes(entity.getDescription())
                                .isDeleted(false)
                                .build();
                        ExpenseEntity savedExpense = expenseRepository.save(expense);
                        entity.setExpenseId(savedExpense.getId());
                    } else if ("credit".equalsIgnoreCase(entity.getBaseType())) {
                        UUID payClientId = rule.getClientId();
                        if (payClientId == null) {
                            ClientEntity defaultClient = clientRepository.findByCompanyIdAndIsDeletedFalse(companyId).stream().findFirst().orElseGet(() ->
                                    clientRepository.save(ClientEntity.builder()
                                            .companyId(companyId)
                                            .name("Bank Deposit Client")
                                            .clientNumber(sequenceGeneratorService.generateSequence("client_" + companyId))
                                            .build())
                            );
                            payClientId = defaultClient.getId();
                        }
                        PaymentEntity payment = PaymentEntity.builder()
                                .companyId(companyId)
                                .userId(entity.getUserId())
                                .clientId(payClientId)
                                .number(sequenceGeneratorService.generateSequence("payment_" + companyId))
                                .amount(entity.getAmount())
                                .applied(BigDecimal.ZERO)
                                .refunded(BigDecimal.ZERO)
                                .date(entity.getDate())
                                .typeId("bank_transfer")
                                .transactionReference("Rule Auto-Convert " + rule.getId())
                                .isDeleted(false)
                                .build();
                        PaymentEntity savedPayment = paymentRepository.save(payment);
                        entity.setPaymentId(savedPayment.getId());
                    }
                }
                break;
            }
        }
    }

    public BankTransactionDto toDto(BankTransactionEntity entity) {
        if (entity == null) return null;
        return BankTransactionDto.builder()
                .id(entity.getId())
                .transactionId(entity.getId())
                .userId(entity.getUserId())
                .bankIntegrationId(entity.getBankIntegrationId())
                .bankTransactionRuleId(entity.getBankTransactionRuleId())
                .date(entity.getDate())
                .amount(entity.getAmount())
                .currencyId(entity.getCurrencyId())
                .description(entity.getDescription())
                .baseType(entity.getBaseType())
                .statusId(entity.getStatusId())
                .paymentId(entity.getPaymentId())
                .expenseId(entity.getExpenseId())
                .vendorId(entity.getVendorId())
                .categoryId(entity.getCategoryId())
                .invoicelyCategoryId(entity.getCategoryId())
                .participant(entity.getParticipant())
                .participantName(entity.getParticipantName())
                .invoiceIds(entity.getInvoiceIds())
                .accountType(entity.getAccountType())
                .categoryType(entity.getCategoryType())
                .isDeleted(Boolean.TRUE.equals(entity.getIsDeleted()))
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
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
