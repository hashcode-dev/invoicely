package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.procurement.PurchaseOrderBulkActionRequest;
import com.hashcode.invoicely.dto.procurement.PurchaseOrderDto;
import com.hashcode.invoicely.dto.procurement.PurchaseOrderItemDto;
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
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final VendorRepository vendorRepository;
    private final ExpenseRepository expenseRepository;
    private final VendorService vendorService;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Transactional(readOnly = true)
    public GenericManyResponse<PurchaseOrderDto> getPurchaseOrders(int page, int perPage, String status, UUID vendorId, String filter, String sort) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 50;

        Sort sorting = Sort.by(Sort.Direction.DESC, "updatedAt");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split("\\|");
            if (parts.length == 2) {
                sorting = Sort.by("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);

        Specification<PurchaseOrderEntity> spec = (root, query, cb) -> {
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

            if (StringUtils.hasText(filter)) {
                String pattern = "%" + filter.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("number")), pattern),
                        cb.like(cb.lower(root.get("poNumber")), pattern),
                        cb.like(cb.lower(root.get("publicNotes")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<PurchaseOrderEntity> paged = purchaseOrderRepository.findAll(spec, pageable);
        List<PurchaseOrderDto> dtos = paged.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return GenericManyResponse.of(dtos, paged.getTotalElements(), paged.getTotalPages(), pageSize);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto getBlankPurchaseOrder(UUID vendorId) {
        UUID companyId = getRequiredCompanyId();
        String nextNumber = sequenceGeneratorService.generateSequence("purchase_order_" + companyId);

        PurchaseOrderItemDto defaultItem = PurchaseOrderItemDto.builder()
                .productKey("")
                .notes("")
                .cost(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .quantity(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP))
                .discount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxRate1(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxRate2(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxRate3(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .lineTotal(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .sortOrder(0)
                .build();

        return PurchaseOrderDto.builder()
                .number(nextNumber)
                .date(LocalDate.now().toString())
                .dueDate(LocalDate.now().plusDays(30).toString())
                .statusId("draft")
                .vendorId(vendorId)
                .amount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .balance(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .paidToDate(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .discount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .totalTaxes(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .exchangeRate(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP))
                .designId("clean")
                .isDeleted(false)
                .lineItems(List.of(defaultItem))
                .documents(Collections.emptyList())
                .build();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrderById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        PurchaseOrderEntity entity = purchaseOrderRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found: " + id));
        return toDto(entity);
    }

    @Transactional
    public PurchaseOrderDto createPurchaseOrder(PurchaseOrderDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        if (dto.getVendorId() == null) {
            throw new IllegalArgumentException("Vendor ID is required for purchase order");
        }

        VendorEntity vendor = vendorRepository.findByCompanyIdAndId(companyId, dto.getVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + dto.getVendorId()));

        String poNumber = StringUtils.hasText(dto.getNumber())
                ? dto.getNumber().trim()
                : sequenceGeneratorService.generateSequence("purchase_order_" + companyId);

        LocalDate date = LocalDate.now();
        if (StringUtils.hasText(dto.getDate())) {
            try {
                date = LocalDate.parse(dto.getDate().trim());
            } catch (DateTimeParseException e) {
                log.warn("Invalid PO date: {}", dto.getDate());
            }
        }

        LocalDate dueDate = null;
        if (StringUtils.hasText(dto.getDueDate())) {
            try {
                dueDate = LocalDate.parse(dto.getDueDate().trim());
            } catch (DateTimeParseException e) {
                log.warn("Invalid PO due date: {}", dto.getDueDate());
            }
        }

        PurchaseOrderEntity entity = PurchaseOrderEntity.builder()
                .companyId(companyId)
                .vendorId(vendor.getId())
                .clientId(dto.getClientId())
                .userId(userId)
                .assignedUserId(dto.getAssignedUserId())
                .number(poNumber)
                .poNumber(dto.getPoNumber())
                .date(date)
                .dueDate(dueDate)
                .statusId(StringUtils.hasText(dto.getStatusId()) ? dto.getStatusId().trim() : "draft")
                .discount(dto.getDiscount() != null ? dto.getDiscount().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .isAmountDiscount(dto.isAmountDiscount())
                .terms(dto.getTerms())
                .publicNotes(dto.getPublicNotes())
                .privateNotes(dto.getPrivateNotes())
                .footer(dto.getFooter())
                .usesInclusiveTaxes(dto.isUsesInclusiveTaxes())
                .taxName1(dto.getTaxName1())
                .taxRate1(dto.getTaxRate1() != null ? dto.getTaxRate1().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxName2(dto.getTaxName2())
                .taxRate2(dto.getTaxRate2() != null ? dto.getTaxRate2().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxName3(dto.getTaxName3())
                .taxRate3(dto.getTaxRate3() != null ? dto.getTaxRate3().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .customValue1(dto.getCustomValue1())
                .customValue2(dto.getCustomValue2())
                .customValue3(dto.getCustomValue3())
                .customValue4(dto.getCustomValue4())
                .exchangeRate(dto.getExchangeRate() != null ? dto.getExchangeRate().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP))
                .designId(StringUtils.hasText(dto.getDesignId()) ? dto.getDesignId() : "clean")
                .isDeleted(false)
                .build();

        calculateAndSetLineItems(entity, dto.getLineItems());

        PurchaseOrderEntity saved = purchaseOrderRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public PurchaseOrderDto updatePurchaseOrder(UUID id, PurchaseOrderDto dto) {
        UUID companyId = getRequiredCompanyId();
        PurchaseOrderEntity entity = purchaseOrderRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found: " + id));

        if (dto.getVendorId() != null) {
            VendorEntity vendor = vendorRepository.findByCompanyIdAndId(companyId, dto.getVendorId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + dto.getVendorId()));
            entity.setVendorId(vendor.getId());
        }

        if (dto.getClientId() != null) entity.setClientId(dto.getClientId());
        if (dto.getAssignedUserId() != null) entity.setAssignedUserId(dto.getAssignedUserId());
        if (StringUtils.hasText(dto.getNumber())) entity.setNumber(dto.getNumber().trim());
        if (dto.getPoNumber() != null) entity.setPoNumber(dto.getPoNumber());
        if (dto.getStatusId() != null) entity.setStatusId(dto.getStatusId().trim());

        if (StringUtils.hasText(dto.getDate())) {
            try {
                entity.setDate(LocalDate.parse(dto.getDate().trim()));
            } catch (DateTimeParseException ignored) {}
        }
        if (StringUtils.hasText(dto.getDueDate())) {
            try {
                entity.setDueDate(LocalDate.parse(dto.getDueDate().trim()));
            } catch (DateTimeParseException ignored) {}
        }

        if (dto.getDiscount() != null) entity.setDiscount(dto.getDiscount().setScale(4, RoundingMode.HALF_UP));
        entity.setAmountDiscount(dto.isAmountDiscount());
        if (dto.getTerms() != null) entity.setTerms(dto.getTerms());
        if (dto.getPublicNotes() != null) entity.setPublicNotes(dto.getPublicNotes());
        if (dto.getPrivateNotes() != null) entity.setPrivateNotes(dto.getPrivateNotes());
        if (dto.getFooter() != null) entity.setFooter(dto.getFooter());
        entity.setUsesInclusiveTaxes(dto.isUsesInclusiveTaxes());

        if (dto.getTaxName1() != null) entity.setTaxName1(dto.getTaxName1());
        if (dto.getTaxRate1() != null) entity.setTaxRate1(dto.getTaxRate1().setScale(4, RoundingMode.HALF_UP));
        if (dto.getTaxName2() != null) entity.setTaxName2(dto.getTaxName2());
        if (dto.getTaxRate2() != null) entity.setTaxRate2(dto.getTaxRate2().setScale(4, RoundingMode.HALF_UP));
        if (dto.getTaxName3() != null) entity.setTaxName3(dto.getTaxName3());
        if (dto.getTaxRate3() != null) entity.setTaxRate3(dto.getTaxRate3().setScale(4, RoundingMode.HALF_UP));

        if (dto.getCustomValue1() != null) entity.setCustomValue1(dto.getCustomValue1());
        if (dto.getCustomValue2() != null) entity.setCustomValue2(dto.getCustomValue2());
        if (dto.getCustomValue3() != null) entity.setCustomValue3(dto.getCustomValue3());
        if (dto.getCustomValue4() != null) entity.setCustomValue4(dto.getCustomValue4());
        if (dto.getExchangeRate() != null) entity.setExchangeRate(dto.getExchangeRate().setScale(4, RoundingMode.HALF_UP));

        if (dto.getLineItems() != null) {
            calculateAndSetLineItems(entity, dto.getLineItems());
        }

        PurchaseOrderEntity updated = purchaseOrderRepository.save(entity);
        return toDto(updated);
    }

    @Transactional
    public void deletePurchaseOrder(UUID id) {
        UUID companyId = getRequiredCompanyId();
        PurchaseOrderEntity entity = purchaseOrderRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found: " + id));
        entity.setDeleted(true);
        purchaseOrderRepository.save(entity);
    }

    @Transactional
    public PurchaseOrderDto markSent(UUID id) {
        UUID companyId = getRequiredCompanyId();
        PurchaseOrderEntity entity = purchaseOrderRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found: " + id));
        entity.setStatusId("sent");
        return toDto(purchaseOrderRepository.save(entity));
    }

    @Transactional
    public List<PurchaseOrderDto> bulkAction(PurchaseOrderBulkActionRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return List.of();
        }
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();
        List<PurchaseOrderEntity> entities = purchaseOrderRepository.findByCompanyIdAndIdIn(companyId, request.getIds());

        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";
        for (PurchaseOrderEntity entity : entities) {
            switch (action) {
                case "archive" -> entity.setArchivedAt(Instant.now());
                case "restore" -> {
                    entity.setArchivedAt(null);
                    entity.setDeleted(false);
                }
                case "delete" -> entity.setDeleted(true);
                case "mark_sent" -> entity.setStatusId("sent");
                case "expense" -> {
                    // Convert PO to Expense
                    ExpenseEntity exp = ExpenseEntity.builder()
                            .companyId(companyId)
                            .userId(userId)
                            .vendorId(entity.getVendorId())
                            .clientId(entity.getClientId())
                            .purchaseOrderId(entity.getId())
                            .number(sequenceGeneratorService.generateSequence("expense_" + companyId))
                            .amount(entity.getAmount())
                            .foreignAmount(entity.getAmount())
                            .exchangeRate(entity.getExchangeRate())
                            .currencyId("USD")
                            .date(LocalDate.now())
                            .taxName1(entity.getTaxName1())
                            .taxRate1(entity.getTaxRate1())
                            .taxName2(entity.getTaxName2())
                            .taxRate2(entity.getTaxRate2())
                            .taxName3(entity.getTaxName3())
                            .taxRate3(entity.getTaxRate3())
                            .taxAmount1(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                            .taxAmount2(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                            .taxAmount3(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                            .publicNotes("Created from PO #" + entity.getNumber())
                            .statusId("logged")
                            .isDeleted(false)
                            .build();

                    ExpenseEntity savedExp = expenseRepository.save(exp);
                    entity.setExpenseId(savedExp.getId());
                    entity.setStatusId("converted_to_expense");
                }
                default -> throw new IllegalArgumentException("Unsupported bulk action: " + action);
            }
        }
        purchaseOrderRepository.saveAll(entities);
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    private void calculateAndSetLineItems(PurchaseOrderEntity po, List<PurchaseOrderItemDto> itemDtos) {
        po.getLineItems().clear();

        BigDecimal subtotal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalLineTaxes = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        int order = 0;
        if (itemDtos != null) {
            for (PurchaseOrderItemDto itemDto : itemDtos) {
                BigDecimal cost = itemDto.getCost() != null ? itemDto.getCost().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
                BigDecimal qty = itemDto.getQuantity() != null ? itemDto.getQuantity().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
                BigDecimal lineGross = cost.multiply(qty).setScale(4, RoundingMode.HALF_UP);

                BigDecimal discount = itemDto.getDiscount() != null ? itemDto.getDiscount().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
                BigDecimal lineNet;
                if (itemDto.isAmountDiscount()) {
                    lineNet = lineGross.subtract(discount);
                } else {
                    BigDecimal discRatio = discount.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                    lineNet = lineGross.subtract(lineGross.multiply(discRatio));
                }
                if (lineNet.compareTo(BigDecimal.ZERO) < 0) lineNet = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

                BigDecimal r1 = itemDto.getTaxRate1() != null ? itemDto.getTaxRate1() : BigDecimal.ZERO;
                BigDecimal r2 = itemDto.getTaxRate2() != null ? itemDto.getTaxRate2() : BigDecimal.ZERO;
                BigDecimal r3 = itemDto.getTaxRate3() != null ? itemDto.getTaxRate3() : BigDecimal.ZERO;
                BigDecimal totalRate = r1.add(r2).add(r3);
                BigDecimal lineTax = lineNet.multiply(totalRate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)).setScale(4, RoundingMode.HALF_UP);

                BigDecimal lineTotal = lineNet.add(lineTax).setScale(4, RoundingMode.HALF_UP);
                subtotal = subtotal.add(lineNet);
                totalLineTaxes = totalLineTaxes.add(lineTax);

                PurchaseOrderItemEntity itemEntity = PurchaseOrderItemEntity.builder()
                        .productKey(itemDto.getProductKey())
                        .notes(itemDto.getNotes())
                        .cost(cost)
                        .quantity(qty)
                        .discount(discount)
                        .isAmountDiscount(itemDto.isAmountDiscount())
                        .taxName1(itemDto.getTaxName1())
                        .taxRate1(r1.setScale(4, RoundingMode.HALF_UP))
                        .taxName2(itemDto.getTaxName2())
                        .taxRate2(r2.setScale(4, RoundingMode.HALF_UP))
                        .taxName3(itemDto.getTaxName3())
                        .taxRate3(r3.setScale(4, RoundingMode.HALF_UP))
                        .lineTotal(lineTotal)
                        .sortOrder(order++)
                        .build();

                po.addLineItem(itemEntity);
            }
        }

        // Apply PO-level discount
        BigDecimal totalAfterDisc;
        if (po.isAmountDiscount()) {
            totalAfterDisc = subtotal.subtract(po.getDiscount());
        } else {
            BigDecimal discRatio = po.getDiscount().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
            totalAfterDisc = subtotal.subtract(subtotal.multiply(discRatio));
        }
        if (totalAfterDisc.compareTo(BigDecimal.ZERO) < 0) totalAfterDisc = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        BigDecimal poTaxRate = po.getTaxRate1().add(po.getTaxRate2()).add(po.getTaxRate3());
        BigDecimal poTax = totalAfterDisc.multiply(poTaxRate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)).setScale(4, RoundingMode.HALF_UP);

        BigDecimal finalAmount = totalAfterDisc.add(totalLineTaxes).add(poTax).setScale(4, RoundingMode.HALF_UP);
        po.setAmount(finalAmount);
        po.setTotalTaxes(totalLineTaxes.add(poTax).setScale(4, RoundingMode.HALF_UP));
        po.setBalance(finalAmount.subtract(po.getPaidToDate()).setScale(4, RoundingMode.HALF_UP));
    }

    public PurchaseOrderDto toDto(PurchaseOrderEntity entity) {
        if (entity == null) return null;

        List<PurchaseOrderItemDto> items = entity.getLineItems() != null
                ? entity.getLineItems().stream().map(this::toItemDto).collect(Collectors.toList())
                : Collections.emptyList();

        return PurchaseOrderDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .vendorId(entity.getVendorId())
                .clientId(entity.getClientId())
                .number(entity.getNumber())
                .poNumber(entity.getPoNumber())
                .date(entity.getDate() != null ? entity.getDate().toString() : null)
                .dueDate(entity.getDueDate() != null ? entity.getDueDate().toString() : null)
                .statusId(entity.getStatusId())
                .amount(entity.getAmount())
                .balance(entity.getBalance())
                .paidToDate(entity.getPaidToDate())
                .discount(entity.getDiscount())
                .isAmountDiscount(entity.isAmountDiscount())
                .totalTaxes(entity.getTotalTaxes())
                .terms(entity.getTerms())
                .publicNotes(entity.getPublicNotes())
                .privateNotes(entity.getPrivateNotes())
                .footer(entity.getFooter())
                .usesInclusiveTaxes(entity.isUsesInclusiveTaxes())
                .taxName1(entity.getTaxName1())
                .taxRate1(entity.getTaxRate1())
                .taxName2(entity.getTaxName2())
                .taxRate2(entity.getTaxRate2())
                .taxName3(entity.getTaxName3())
                .taxRate3(entity.getTaxRate3())
                .customValue1(entity.getCustomValue1())
                .customValue2(entity.getCustomValue2())
                .customValue3(entity.getCustomValue3())
                .customValue4(entity.getCustomValue4())
                .exchangeRate(entity.getExchangeRate())
                .designId(entity.getDesignId())
                .expenseId(entity.getExpenseId())
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .lineItems(items)
                .vendor(vendorRepository.findById(entity.getVendorId()).map(vendorService::toDto).orElse(null))
                .documents(Collections.emptyList())
                .build();
    }

    public PurchaseOrderItemDto toItemDto(PurchaseOrderItemEntity entity) {
        if (entity == null) return null;
        return PurchaseOrderItemDto.builder()
                .id(entity.getId())
                .productKey(entity.getProductKey())
                .notes(entity.getNotes())
                .cost(entity.getCost())
                .quantity(entity.getQuantity())
                .discount(entity.getDiscount())
                .isAmountDiscount(entity.isAmountDiscount())
                .taxName1(entity.getTaxName1())
                .taxRate1(entity.getTaxRate1())
                .taxName2(entity.getTaxName2())
                .taxRate2(entity.getTaxRate2())
                .taxName3(entity.getTaxName3())
                .taxRate3(entity.getTaxRate3())
                .lineTotal(entity.getLineTotal())
                .sortOrder(entity.getSortOrder())
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
