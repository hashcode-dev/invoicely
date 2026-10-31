package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.InvoiceBulkActionRequest;
import com.hashcode.invoicely.dto.invoice.InvoiceDto;
import com.hashcode.invoicely.dto.invoice.InvoiceItemDto;
import com.hashcode.invoicely.entity.relational.ClientEntity;
import com.hashcode.invoicely.entity.relational.InvoiceEntity;
import com.hashcode.invoicely.entity.relational.InvoiceItemEntity;
import com.hashcode.invoicely.mapper.InvoiceMapper;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.InvoiceRepository;
import com.hashcode.invoicely.service.ClientService;
import com.hashcode.invoicely.service.InvoiceService;
import com.hashcode.invoicely.service.SequenceGeneratorService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final ClientService clientService;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final InvoiceMapper invoiceMapper;

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto getBlankInvoice(UUID clientId) {
        UUID companyId = getRequiredCompanyId();
        String nextNumber = sequenceGeneratorService.generateSequence("invoice_" + companyId);

        InvoiceItemDto defaultItem = InvoiceItemDto.builder()
                .quantity(BigDecimal.ONE)
                .cost(BigDecimal.ZERO)
                .lineTotal(BigDecimal.ZERO)
                .typeId("1")
                .build();

        InvoiceDto.InvoiceDtoBuilder builder = InvoiceDto.builder()
                .number(nextNumber)
                .date(LocalDate.now().toString())
                .dueDate(LocalDate.now().plusDays(30).toString())
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

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        InvoiceEntity entity = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + id));
        return toDto(entity);
    }

    @Override
    @Transactional
    public InvoiceDto createInvoice(InvoiceDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        if (dto.getClientId() == null) {
            throw new IllegalArgumentException("Client ID is required to create an invoice");
        }

        ClientEntity client = clientRepository.findByCompanyIdAndId(companyId, dto.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + dto.getClientId()));

        String invoiceNumber = StringUtils.hasText(dto.getNumber())
                ? dto.getNumber().trim()
                : sequenceGeneratorService.generateSequence("invoice_" + companyId);

        LocalDate invoiceDate = parseDate(dto.getDate(), LocalDate.now());
        LocalDate dueDate = parseDate(dto.getDueDate(), invoiceDate.plusDays(30));
        LocalDate partialDueDate = parseDateOrNull(dto.getPartialDueDate());

        InvoiceEntity entity = InvoiceEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .assignedUserId(dto.getAssignedUserId())
                .clientId(client.getId())
                .recurringId(dto.getRecurringId())
                .number(invoiceNumber)
                .poNumber(dto.getPoNumber())
                .date(invoiceDate)
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
                .paidToDate(BigDecimal.ZERO)
                .isDeleted(false)
                .lineItems(new ArrayList<>())
                .build();

        populateLineItemsAndCalculate(entity, dto.getLineItems());

        client.setBalance(client.getBalance().add(entity.getBalance()));
        clientRepository.save(client);

        InvoiceEntity saved = invoiceRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public InvoiceDto updateInvoice(UUID id, InvoiceDto dto) {
        UUID companyId = getRequiredCompanyId();
        InvoiceEntity entity = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + id));

        BigDecimal oldBalance = entity.getBalance();

        if (StringUtils.hasText(dto.getNumber())) {
            entity.setNumber(dto.getNumber().trim());
        }
        if (dto.getPoNumber() != null) {
            entity.setPoNumber(dto.getPoNumber());
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
        if (dto.getAssignedUserId() != null) {
            entity.setAssignedUserId(dto.getAssignedUserId());
        }
        if (dto.getDiscount() != null) {
            entity.setDiscount(dto.getDiscount());
        }
        entity.setAmountDiscount(dto.isAmountDiscount());
        if (dto.getPartial() != null) {
            entity.setPartial(dto.getPartial());
        }
        if (dto.getTerms() != null) entity.setTerms(dto.getTerms());
        if (dto.getPublicNotes() != null) entity.setPublicNotes(dto.getPublicNotes());
        if (dto.getPrivateNotes() != null) entity.setPrivateNotes(dto.getPrivateNotes());
        if (dto.getFooter() != null) entity.setFooter(dto.getFooter());
        entity.setUsesInclusiveTaxes(dto.isUsesInclusiveTaxes());

        if (dto.getTaxName1() != null) entity.setTaxName1(dto.getTaxName1());
        if (dto.getTaxRate1() != null) entity.setTaxRate1(dto.getTaxRate1());
        if (dto.getTaxName2() != null) entity.setTaxName2(dto.getTaxName2());
        if (dto.getTaxRate2() != null) entity.setTaxRate2(dto.getTaxRate2());
        if (dto.getTaxName3() != null) entity.setTaxName3(dto.getTaxName3());
        if (dto.getTaxRate3() != null) entity.setTaxRate3(dto.getTaxRate3());

        if (dto.getCustomValue1() != null) entity.setCustomValue1(dto.getCustomValue1());
        if (dto.getCustomValue2() != null) entity.setCustomValue2(dto.getCustomValue2());
        if (dto.getCustomValue3() != null) entity.setCustomValue3(dto.getCustomValue3());
        if (dto.getCustomValue4() != null) entity.setCustomValue4(dto.getCustomValue4());
        if (dto.getExchangeRate() != null) entity.setExchangeRate(dto.getExchangeRate());
        if (dto.getDesignId() != null) entity.setDesignId(dto.getDesignId());

        if (dto.getLineItems() != null) {
            entity.getLineItems().clear();
            populateLineItemsAndCalculate(entity, dto.getLineItems());
        }

        BigDecimal balanceDiff = entity.getBalance().subtract(oldBalance);
        if (balanceDiff.compareTo(BigDecimal.ZERO) != 0) {
            clientRepository.findByCompanyIdAndId(companyId, entity.getClientId()).ifPresent(client -> {
                client.setBalance(client.getBalance().add(balanceDiff));
                clientRepository.save(client);
            });
        }

        InvoiceEntity updated = invoiceRepository.save(entity);
        return toDto(updated);
    }

    @Override
    @Transactional
    public List<InvoiceDto> bulkAction(InvoiceBulkActionRequest request) {
        UUID companyId = getRequiredCompanyId();
        List<UUID> ids = request.getIds();
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<InvoiceEntity> entities = invoiceRepository.findByCompanyIdAndIdIn(companyId, ids);
        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";

        List<InvoiceEntity> resultEntities = new ArrayList<>();

        switch (action) {
            case "archive":
                Instant now = Instant.now();
                for (InvoiceEntity invoice : entities) {
                    invoice.setArchivedAt(now);
                    invoice.setDeleted(false);
                }
                resultEntities = invoiceRepository.saveAll(entities);
                break;

            case "restore":
                for (InvoiceEntity invoice : entities) {
                    invoice.setArchivedAt(null);
                    invoice.setDeleted(false);
                }
                resultEntities = invoiceRepository.saveAll(entities);
                break;

            case "delete":
                Instant delTime = Instant.now();
                for (InvoiceEntity invoice : entities) {
                    invoice.setDeleted(true);
                    invoice.setArchivedAt(delTime);
                    clientRepository.findByCompanyIdAndId(companyId, invoice.getClientId()).ifPresent(client -> {
                        client.setBalance(client.getBalance().subtract(invoice.getBalance()).max(BigDecimal.ZERO));
                        clientRepository.save(client);
                    });
                }
                resultEntities = invoiceRepository.saveAll(entities);
                break;

            case "mark_paid":
                for (InvoiceEntity invoice : entities) {
                    invoice.setStatusId("paid");
                    BigDecimal prevBalance = invoice.getBalance();
                    invoice.setPaidToDate(invoice.getAmount());
                    invoice.setBalance(BigDecimal.ZERO);
                    clientRepository.findByCompanyIdAndId(companyId, invoice.getClientId()).ifPresent(client -> {
                        client.setBalance(client.getBalance().subtract(prevBalance).max(BigDecimal.ZERO));
                        client.setPaidToDate(client.getPaidToDate().add(prevBalance));
                        clientRepository.save(client);
                    });
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

    @Override
    @Transactional
    public InvoiceDto cloneInvoice(UUID id) {
        UUID companyId = getRequiredCompanyId();
        InvoiceEntity original = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + id));

        InvoiceEntity clone = cloneInvoiceEntity(original);
        clientRepository.findByCompanyIdAndId(companyId, clone.getClientId()).ifPresent(client -> {
            client.setBalance(client.getBalance().add(clone.getBalance()));
            clientRepository.save(client);
        });

        return toDto(clone);
    }

    @Override
    @Transactional
    public InvoiceDto markSent(UUID id) {
        UUID companyId = getRequiredCompanyId();
        InvoiceEntity invoice = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + id));

        invoice.setStatusId("sent");
        InvoiceEntity saved = invoiceRepository.save(invoice);
        return toDto(saved);
    }

    @Override
    @Transactional
    public InvoiceDto markPaid(UUID id) {
        UUID companyId = getRequiredCompanyId();
        InvoiceEntity invoice = invoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + id));

        invoice.setStatusId("paid");
        BigDecimal prevBalance = invoice.getBalance();
        invoice.setPaidToDate(invoice.getAmount());
        invoice.setBalance(BigDecimal.ZERO);

        clientRepository.findByCompanyIdAndId(companyId, invoice.getClientId()).ifPresent(client -> {
            client.setBalance(client.getBalance().subtract(prevBalance).max(BigDecimal.ZERO));
            client.setPaidToDate(client.getPaidToDate().add(prevBalance));
            clientRepository.save(client);
        });

        InvoiceEntity saved = invoiceRepository.save(invoice);
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

    @Override
    public InvoiceDto toDto(InvoiceEntity entity) {
        return invoiceMapper.toDto(entity);
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
