package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.InvoiceBulkActionRequest;
import com.hashcode.invoicely.dto.invoice.InvoiceDto;
import com.hashcode.invoicely.dto.invoice.InvoiceItemDto;
import com.hashcode.invoicely.entity.relational.ClientEntity;
import com.hashcode.invoicely.entity.relational.InvoiceEntity;
import com.hashcode.invoicely.entity.relational.InvoiceItemEntity;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.InvoiceRepository;
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
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final ClientService clientService;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Transactional(readOnly = true)
    public GenericManyResponse<InvoiceDto> getInvoices(int page, int perPage, String status, UUID clientId, String filter, String sort) {
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

        Specification<InvoiceEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));

            if (clientId != null) {
                predicates.add(cb.equal(root.get("clientId"), clientId));
            }

            if ("archived".equalsIgnoreCase(status)) {
                predicates.add(cb.isNotNull(root.get("archivedAt")));
                predicates.add(cb.isFalse(root.get("isDeleted")));
            } else if ("deleted".equalsIgnoreCase(status)) {
                predicates.add(cb.isTrue(root.get("isDeleted")));
            } else if (StringUtils.hasText(status)) {
                if (!"all".equalsIgnoreCase(status)) {
                    predicates.add(cb.equal(cb.lower(root.get("statusId")), status.toLowerCase()));
                    predicates.add(cb.isFalse(root.get("isDeleted")));
                }
            } else {
                predicates.add(cb.isFalse(root.get("isDeleted")));
                predicates.add(cb.isNull(root.get("archivedAt")));
            }

            if (StringUtils.hasText(filter)) {
                String searchPattern = "%" + filter.trim().toLowerCase() + "%";
                Predicate numMatch = cb.like(cb.lower(root.get("number")), searchPattern);
                Predicate poMatch = cb.like(cb.lower(root.get("poNumber")), searchPattern);
                predicates.add(cb.or(numMatch, poMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<InvoiceEntity> invoicePage = invoiceRepository.findAll(spec, pageable);
        List<InvoiceDto> dtos = invoicePage.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return GenericManyResponse.of(dtos, invoicePage.getTotalElements(), page, pageSize);
    }

    @Transactional(readOnly = true)
    public InvoiceDto getBlankInvoice(UUID clientId) {
        UUID companyId = getRequiredCompanyId();
        String nextNumber = sequenceGeneratorService.generateSequence("invoice_" + companyId);

        InvoiceItemDto defaultItem = InvoiceItemDto.builder()
                .quantity(BigDecimal.ONE)
                .cost(BigDecimal.ZERO)
                .lineTotal(BigDecimal.ZERO)
                .grossLineTotal(BigDecimal.ZERO)
                .build();

        LocalDate today = LocalDate.now();
        LocalDate due = today.plusDays(30);

        InvoiceDto.InvoiceDtoBuilder builder = InvoiceDto.builder()
                .number(nextNumber)
                .date(today.toString())
                .dueDate(due.toString())
                .statusId("draft")
                .amount(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .paidToDate(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .exchangeRate(BigDecimal.ONE)
                .designId("clean")
                .lineItems(Collections.singletonList(defaultItem));

        if (clientId != null) {
            builder.clientId(clientId);
            clientRepository.findByCompanyIdAndId(companyId, clientId)
                    .ifPresent(c -> builder.client(clientService.toDto(c)));
        }

        return builder.build();
    }

    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        InvoiceEntity entity = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
        return toDto(entity);
    }

    @Transactional
    public InvoiceDto createInvoice(InvoiceDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        if (dto.getClientId() == null) {
            throw new IllegalArgumentException("client_id is required to create an invoice");
        }

        ClientEntity client = clientRepository.findByCompanyIdAndId(companyId, dto.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + dto.getClientId()));

        String invoiceNumber = StringUtils.hasText(dto.getNumber())
                ? dto.getNumber().trim()
                : sequenceGeneratorService.generateSequence("invoice_" + companyId);

        LocalDate date = parseDate(dto.getDate(), LocalDate.now());
        LocalDate dueDate = parseDate(dto.getDueDate(), date.plusDays(30));
        LocalDate partialDueDate = parseDateOrNull(dto.getPartialDueDate());

        InvoiceEntity entity = InvoiceEntity.builder()
                .companyId(companyId)
                .clientId(client.getId())
                .userId(userId)
                .assignedUserId(dto.getAssignedUserId())
                .number(invoiceNumber)
                .poNumber(dto.getPoNumber())
                .date(date)
                .dueDate(dueDate)
                .partialDueDate(partialDueDate)
                .statusId(StringUtils.hasText(dto.getStatusId()) ? dto.getStatusId() : "draft")
                .discount(dto.getDiscount() != null ? dto.getDiscount() : BigDecimal.ZERO)
                .isAmountDiscount(dto.isAmountDiscount())
                .partial(dto.getPartial() != null ? dto.getPartial() : BigDecimal.ZERO)
                .terms(dto.getTerms())
                .publicNotes(dto.getPublicNotes())
                .privateNotes(dto.getPrivateNotes())
                .footer(dto.getFooter())
                .usesInclusiveTaxes(dto.isUsesInclusiveTaxes())
                .taxName1(dto.getTaxName1())
                .taxRate1(dto.getTaxRate1() != null ? dto.getTaxRate1() : BigDecimal.ZERO)
                .taxName2(dto.getTaxName2())
                .taxRate2(dto.getTaxRate2() != null ? dto.getTaxRate2() : BigDecimal.ZERO)
                .taxName3(dto.getTaxName3())
                .taxRate3(dto.getTaxRate3() != null ? dto.getTaxRate3() : BigDecimal.ZERO)
                .customValue1(dto.getCustomValue1())
                .customValue2(dto.getCustomValue2())
                .customValue3(dto.getCustomValue3())
                .customValue4(dto.getCustomValue4())
                .exchangeRate(dto.getExchangeRate() != null ? dto.getExchangeRate() : BigDecimal.ONE)
                .designId(StringUtils.hasText(dto.getDesignId()) ? dto.getDesignId() : "clean")
                .isDeleted(false)
                .lineItems(new ArrayList<>())
                .build();

        populateLineItemsAndCalculate(entity, dto.getLineItems());

        InvoiceEntity saved = invoiceRepository.save(entity);

        client.setBalance(client.getBalance().add(saved.getBalance()));
        clientRepository.save(client);

        return toDto(saved);
    }

    @Transactional
    public InvoiceDto updateInvoice(UUID id, InvoiceDto dto) {
        UUID companyId = getRequiredCompanyId();
        InvoiceEntity entity = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));

        BigDecimal oldBalance = entity.getBalance();

        if (StringUtils.hasText(dto.getNumber())) {
            entity.setNumber(dto.getNumber().trim());
        }
        if (StringUtils.hasText(dto.getPoNumber())) {
            entity.setPoNumber(dto.getPoNumber().trim());
        }
        if (StringUtils.hasText(dto.getDate())) {
            entity.setDate(parseDate(dto.getDate(), entity.getDate()));
        }
        if (StringUtils.hasText(dto.getDueDate())) {
            entity.setDueDate(parseDate(dto.getDueDate(), entity.getDueDate()));
        }
        if (dto.getPartialDueDate() != null) {
            entity.setPartialDueDate(parseDateOrNull(dto.getPartialDueDate()));
        }
        if (StringUtils.hasText(dto.getStatusId())) {
            entity.setStatusId(dto.getStatusId());
        }
        if (dto.getDiscount() != null) {
            entity.setDiscount(dto.getDiscount());
        }
        entity.setAmountDiscount(dto.isAmountDiscount());
        if (dto.getPartial() != null) {
            entity.setPartial(dto.getPartial());
        }
        entity.setTerms(dto.getTerms());
        entity.setPublicNotes(dto.getPublicNotes());
        entity.setPrivateNotes(dto.getPrivateNotes());
        entity.setFooter(dto.getFooter());
        entity.setUsesInclusiveTaxes(dto.isUsesInclusiveTaxes());
        entity.setTaxName1(dto.getTaxName1());
        if (dto.getTaxRate1() != null) entity.setTaxRate1(dto.getTaxRate1());
        entity.setTaxName2(dto.getTaxName2());
        if (dto.getTaxRate2() != null) entity.setTaxRate2(dto.getTaxRate2());
        entity.setTaxName3(dto.getTaxName3());
        if (dto.getTaxRate3() != null) entity.setTaxRate3(dto.getTaxRate3());

        if (dto.getLineItems() != null) {
            entity.getLineItems().clear();
            populateLineItemsAndCalculate(entity, dto.getLineItems());
        }

        InvoiceEntity updated = invoiceRepository.save(entity);

        BigDecimal balanceDiff = updated.getBalance().subtract(oldBalance);
        if (balanceDiff.compareTo(BigDecimal.ZERO) != 0) {
            clientRepository.findByCompanyIdAndId(companyId, updated.getClientId()).ifPresent(c -> {
                c.setBalance(c.getBalance().add(balanceDiff));
                clientRepository.save(c);
            });
        }

        return toDto(updated);
    }

    @Transactional
    public List<InvoiceDto> bulkAction(InvoiceBulkActionRequest request) {
        UUID companyId = getRequiredCompanyId();
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return Collections.emptyList();
        }

        List<InvoiceEntity> entities = invoiceRepository.findByCompanyIdAndIdIn(companyId, request.getIds());
        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";

        List<InvoiceEntity> resultEntities = new ArrayList<>();

        switch (action) {
            case "archive":
                Instant now = Instant.now();
                for (InvoiceEntity inv : entities) {
                    inv.setArchivedAt(now);
                    inv.setDeleted(false);
                }
                resultEntities = invoiceRepository.saveAll(entities);
                break;

            case "restore":
                for (InvoiceEntity inv : entities) {
                    inv.setArchivedAt(null);
                    inv.setDeleted(false);
                }
                resultEntities = invoiceRepository.saveAll(entities);
                break;

            case "delete":
                Instant delTime = Instant.now();
                for (InvoiceEntity inv : entities) {
                    inv.setDeleted(true);
                    inv.setArchivedAt(delTime);
                }
                resultEntities = invoiceRepository.saveAll(entities);
                break;

            case "mark_sent":
                for (InvoiceEntity inv : entities) {
                    inv.setStatusId("sent");
                }
                resultEntities = invoiceRepository.saveAll(entities);
                break;

            case "mark_paid":
                for (InvoiceEntity inv : entities) {
                    inv.setStatusId("paid");
                    inv.setPaidToDate(inv.getAmount());
                    inv.setBalance(BigDecimal.ZERO);
                }
                resultEntities = invoiceRepository.saveAll(entities);
                break;

            case "clone":
                for (InvoiceEntity original : entities) {
                    resultEntities.add(cloneInvoiceEntity(original));
                }
                break;

            default:
                resultEntities = entities;
                break;
        }

        return resultEntities.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public InvoiceDto cloneInvoice(UUID id) {
        UUID companyId = getRequiredCompanyId();
        InvoiceEntity original = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
        InvoiceEntity cloned = cloneInvoiceEntity(original);
        return toDto(cloned);
    }

    @Transactional
    public InvoiceDto markSent(UUID id) {
        UUID companyId = getRequiredCompanyId();
        InvoiceEntity entity = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
        entity.setStatusId("sent");
        return toDto(invoiceRepository.save(entity));
    }

    @Transactional
    public InvoiceDto markPaid(UUID id) {
        UUID companyId = getRequiredCompanyId();
        InvoiceEntity entity = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));

        BigDecimal originalBalance = entity.getBalance();
        entity.setStatusId("paid");
        entity.setPaidToDate(entity.getAmount());
        entity.setBalance(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        InvoiceEntity saved = invoiceRepository.save(entity);

        clientRepository.findByCompanyIdAndId(companyId, entity.getClientId()).ifPresent(c -> {
            c.setBalance(c.getBalance().subtract(originalBalance));
            c.setPaidToDate(c.getPaidToDate().add(originalBalance));
            clientRepository.save(c);
        });

        return toDto(saved);
    }

    private InvoiceEntity cloneInvoiceEntity(InvoiceEntity original) {
        UUID companyId = original.getCompanyId();
        String nextNumber = sequenceGeneratorService.generateSequence("invoice_" + companyId);

        InvoiceEntity clone = InvoiceEntity.builder()
                .companyId(companyId)
                .clientId(original.getClientId())
                .userId(original.getUserId())
                .assignedUserId(original.getAssignedUserId())
                .number(nextNumber)
                .poNumber(original.getPoNumber())
                .date(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .statusId("draft")
                .amount(original.getAmount())
                .balance(original.getAmount())
                .paidToDate(BigDecimal.ZERO)
                .discount(original.getDiscount())
                .isAmountDiscount(original.isAmountDiscount())
                .totalTaxes(original.getTotalTaxes())
                .terms(original.getTerms())
                .publicNotes(original.getPublicNotes())
                .privateNotes(original.getPrivateNotes())
                .footer(original.getFooter())
                .usesInclusiveTaxes(original.isUsesInclusiveTaxes())
                .taxName1(original.getTaxName1())
                .taxRate1(original.getTaxRate1())
                .taxName2(original.getTaxName2())
                .taxRate2(original.getTaxRate2())
                .exchangeRate(original.getExchangeRate())
                .designId(original.getDesignId())
                .isDeleted(false)
                .lineItems(new ArrayList<>())
                .build();

        for (InvoiceItemEntity item : original.getLineItems()) {
            InvoiceItemEntity itemClone = InvoiceItemEntity.builder()
                    .productKey(item.getProductKey())
                    .notes(item.getNotes())
                    .cost(item.getCost())
                    .quantity(item.getQuantity())
                    .discount(item.getDiscount())
                    .isAmountDiscount(item.isAmountDiscount())
                    .lineTotal(item.getLineTotal())
                    .grossLineTotal(item.getGrossLineTotal())
                    .taxName1(item.getTaxName1())
                    .taxRate1(item.getTaxRate1())
                    .taxName2(item.getTaxName2())
                    .taxRate2(item.getTaxRate2())
                    .taxName3(item.getTaxName3())
                    .taxRate3(item.getTaxRate3())
                    .sortId(item.getSortId())
                    .build();
            clone.addLineItem(itemClone);
        }

        return invoiceRepository.save(clone);
    }

    private void populateLineItemsAndCalculate(InvoiceEntity entity, List<InvoiceItemDto> itemDtos) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalLineTaxes = BigDecimal.ZERO;

        if (itemDtos != null && !itemDtos.isEmpty()) {
            int sort = 0;
            for (InvoiceItemDto itemDto : itemDtos) {
                BigDecimal qty = itemDto.getQuantity() != null ? itemDto.getQuantity() : BigDecimal.ONE;
                BigDecimal cost = itemDto.getCost() != null ? itemDto.getCost() : BigDecimal.ZERO;
                BigDecimal rawLine = qty.multiply(cost);

                BigDecimal discount = itemDto.getDiscount() != null ? itemDto.getDiscount() : BigDecimal.ZERO;
                BigDecimal lineDiscAmount = itemDto.isAmountDiscount()
                        ? discount
                        : rawLine.multiply(discount).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

                BigDecimal netLineTotal = rawLine.subtract(lineDiscAmount).max(BigDecimal.ZERO);

                BigDecimal t1 = itemDto.getTaxRate1() != null ? itemDto.getTaxRate1() : BigDecimal.ZERO;
                BigDecimal t2 = itemDto.getTaxRate2() != null ? itemDto.getTaxRate2() : BigDecimal.ZERO;
                BigDecimal t3 = itemDto.getTaxRate3() != null ? itemDto.getTaxRate3() : BigDecimal.ZERO;
                BigDecimal taxRateSum = t1.add(t2).add(t3);

                BigDecimal lineTaxAmount = netLineTotal.multiply(taxRateSum).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                BigDecimal grossLineTotal = netLineTotal.add(lineTaxAmount);

                subtotal = subtotal.add(netLineTotal);
                totalLineTaxes = totalLineTaxes.add(lineTaxAmount);

                InvoiceItemEntity itemEntity = InvoiceItemEntity.builder()
                        .productKey(itemDto.getProductKey())
                        .notes(itemDto.getNotes())
                        .cost(cost)
                        .quantity(qty)
                        .discount(discount)
                        .isAmountDiscount(itemDto.isAmountDiscount())
                        .lineTotal(netLineTotal)
                        .grossLineTotal(grossLineTotal)
                        .taxName1(itemDto.getTaxName1())
                        .taxRate1(t1)
                        .taxName2(itemDto.getTaxName2())
                        .taxRate2(t2)
                        .taxName3(itemDto.getTaxName3())
                        .taxRate3(t3)
                        .typeId(StringUtils.hasText(itemDto.getTypeId()) ? itemDto.getTypeId() : "1")
                        .sortId(sort++)
                        .build();

                entity.addLineItem(itemEntity);
            }
        }

        BigDecimal invDiscount = entity.getDiscount() != null ? entity.getDiscount() : BigDecimal.ZERO;
        BigDecimal invDiscountAmount = entity.isAmountDiscount()
                ? invDiscount
                : subtotal.multiply(invDiscount).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal discountedSubtotal = subtotal.subtract(invDiscountAmount).max(BigDecimal.ZERO);

        BigDecimal it1 = entity.getTaxRate1() != null ? entity.getTaxRate1() : BigDecimal.ZERO;
        BigDecimal it2 = entity.getTaxRate2() != null ? entity.getTaxRate2() : BigDecimal.ZERO;
        BigDecimal it3 = entity.getTaxRate3() != null ? entity.getTaxRate3() : BigDecimal.ZERO;
        BigDecimal invTaxSum = it1.add(it2).add(it3);

        BigDecimal invoiceTaxAmount = discountedSubtotal.multiply(invTaxSum).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal totalTaxes = totalLineTaxes.add(invoiceTaxAmount);

        BigDecimal totalAmount = discountedSubtotal.add(totalTaxes);
        BigDecimal paid = entity.getPaidToDate() != null ? entity.getPaidToDate() : BigDecimal.ZERO;
        BigDecimal balance = totalAmount.subtract(paid).max(BigDecimal.ZERO);

        entity.setTotalTaxes(totalTaxes.setScale(4, RoundingMode.HALF_UP));
        entity.setAmount(totalAmount.setScale(4, RoundingMode.HALF_UP));
        entity.setBalance(balance.setScale(4, RoundingMode.HALF_UP));
    }

    public InvoiceDto toDto(InvoiceEntity entity) {
        if (entity == null) {
            return null;
        }

        List<InvoiceItemDto> itemDtos = entity.getLineItems() != null
                ? entity.getLineItems().stream().map(this::toItemDto).collect(Collectors.toList())
                : Collections.emptyList();

        return InvoiceDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .clientId(entity.getClientId())
                .recurringId(entity.getRecurringId())
                .number(entity.getNumber())
                .poNumber(entity.getPoNumber())
                .date(entity.getDate() != null ? entity.getDate().toString() : null)
                .dueDate(entity.getDueDate() != null ? entity.getDueDate().toString() : null)
                .partialDueDate(entity.getPartialDueDate() != null ? entity.getPartialDueDate().toString() : null)
                .statusId(entity.getStatusId())
                .amount(entity.getAmount())
                .balance(entity.getBalance())
                .paidToDate(entity.getPaidToDate())
                .discount(entity.getDiscount())
                .isAmountDiscount(entity.isAmountDiscount())
                .partial(entity.getPartial())
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
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .lineItems(itemDtos)
                .invitations(Collections.emptyList())
                .documents(Collections.emptyList())
                .payments(Collections.emptyList())
                .build();
    }

    private InvoiceItemDto toItemDto(InvoiceItemEntity item) {
        return InvoiceItemDto.builder()
                .id(item.getId())
                .productKey(item.getProductKey())
                .notes(item.getNotes())
                .cost(item.getCost())
                .quantity(item.getQuantity())
                .discount(item.getDiscount())
                .isAmountDiscount(item.isAmountDiscount())
                .lineTotal(item.getLineTotal())
                .grossLineTotal(item.getGrossLineTotal())
                .taxName1(item.getTaxName1())
                .taxRate1(item.getTaxRate1())
                .taxName2(item.getTaxName2())
                .taxRate2(item.getTaxRate2())
                .taxName3(item.getTaxName3())
                .taxRate3(item.getTaxRate3())
                .typeId(item.getTypeId())
                .sortId(item.getSortId())
                .customValue1(item.getCustomValue1())
                .customValue2(item.getCustomValue2())
                .customValue3(item.getCustomValue3())
                .customValue4(item.getCustomValue4())
                .build();
    }

    private LocalDate parseDate(String dateStr, LocalDate fallback) {
        if (!StringUtils.hasText(dateStr)) {
            return fallback;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private LocalDate parseDateOrNull(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalArgumentException("Tenant company context is missing");
        }
        return companyId;
    }
}
