package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.InvoiceDto;
import com.hashcode.invoicely.dto.invoice.QuoteBulkActionRequest;
import com.hashcode.invoicely.dto.invoice.QuoteDto;
import com.hashcode.invoicely.dto.invoice.QuoteItemDto;
import com.hashcode.invoicely.entity.relational.ClientEntity;
import com.hashcode.invoicely.entity.relational.InvoiceEntity;
import com.hashcode.invoicely.entity.relational.InvoiceItemEntity;
import com.hashcode.invoicely.entity.relational.QuoteEntity;
import com.hashcode.invoicely.entity.relational.QuoteItemEntity;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.InvoiceRepository;
import com.hashcode.invoicely.repository.relational.QuoteRepository;
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
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final InvoiceService invoiceService;

    @Transactional(readOnly = true)
    public GenericManyResponse<QuoteDto> getQuotes(int page, int perPage, String status, UUID clientId, String filter, String sort) {
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

        Specification<QuoteEntity> spec = (root, query, cb) -> {
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

        Page<QuoteEntity> quotePage = quoteRepository.findAll(spec, pageable);
        List<QuoteDto> dtos = quotePage.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return GenericManyResponse.of(dtos, quotePage.getTotalElements(), page, pageSize);
    }

    @Transactional(readOnly = true)
    public QuoteDto getBlankQuote(UUID clientId) {
        UUID companyId = getRequiredCompanyId();
        String nextNumber = sequenceGeneratorService.generateSequence("quote_" + companyId);

        QuoteItemDto defaultItem = QuoteItemDto.builder()
                .quantity(BigDecimal.ONE)
                .cost(BigDecimal.ZERO)
                .lineTotal(BigDecimal.ZERO)
                .build();

        LocalDate today = LocalDate.now();
        LocalDate due = today.plusDays(30);

        return QuoteDto.builder()
                .number(nextNumber)
                .clientId(clientId)
                .date(today.toString())
                .dueDate(due.toString())
                .statusId("draft")
                .amount(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .lineItems(Collections.singletonList(defaultItem))
                .build();
    }

    @Transactional(readOnly = true)
    public QuoteDto getQuoteById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        QuoteEntity entity = quoteRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found: " + id));
        return toDto(entity);
    }

    @Transactional
    public QuoteDto createQuote(QuoteDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        if (dto.getClientId() == null) {
            throw new IllegalArgumentException("client_id is required to create a quote");
        }

        ClientEntity client = clientRepository.findByCompanyIdAndId(companyId, dto.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + dto.getClientId()));

        String quoteNumber = StringUtils.hasText(dto.getNumber())
                ? dto.getNumber().trim()
                : sequenceGeneratorService.generateSequence("quote_" + companyId);

        LocalDate date = parseDate(dto.getDate(), LocalDate.now());
        LocalDate dueDate = parseDate(dto.getDueDate(), date.plusDays(30));

        QuoteEntity entity = QuoteEntity.builder()
                .companyId(companyId)
                .clientId(client.getId())
                .userId(userId)
                .assignedUserId(dto.getAssignedUserId())
                .number(quoteNumber)
                .poNumber(dto.getPoNumber())
                .date(date)
                .dueDate(dueDate)
                .statusId(StringUtils.hasText(dto.getStatusId()) ? dto.getStatusId() : "draft")
                .discount(dto.getDiscount() != null ? dto.getDiscount() : BigDecimal.ZERO)
                .isAmountDiscount(dto.isAmountDiscount())
                .terms(dto.getTerms())
                .publicNotes(dto.getPublicNotes())
                .privateNotes(dto.getPrivateNotes())
                .footer(dto.getFooter())
                .usesInclusiveTaxes(dto.isUsesInclusiveTaxes())
                .taxName1(dto.getTaxName1())
                .taxRate1(dto.getTaxRate1() != null ? dto.getTaxRate1() : BigDecimal.ZERO)
                .taxName2(dto.getTaxName2())
                .taxRate2(dto.getTaxRate2() != null ? dto.getTaxRate2() : BigDecimal.ZERO)
                .customValue1(dto.getCustomValue1())
                .customValue2(dto.getCustomValue2())
                .isDeleted(false)
                .lineItems(new ArrayList<>())
                .build();

        populateLineItemsAndCalculate(entity, dto.getLineItems());

        QuoteEntity saved = quoteRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public QuoteDto updateQuote(UUID id, QuoteDto dto) {
        UUID companyId = getRequiredCompanyId();
        QuoteEntity entity = quoteRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found: " + id));

        if (StringUtils.hasText(dto.getNumber())) entity.setNumber(dto.getNumber().trim());
        if (StringUtils.hasText(dto.getPoNumber())) entity.setPoNumber(dto.getPoNumber().trim());
        if (StringUtils.hasText(dto.getDate())) entity.setDate(parseDate(dto.getDate(), entity.getDate()));
        if (StringUtils.hasText(dto.getDueDate())) entity.setDueDate(parseDate(dto.getDueDate(), entity.getDueDate()));
        if (StringUtils.hasText(dto.getStatusId())) entity.setStatusId(dto.getStatusId());
        if (dto.getDiscount() != null) entity.setDiscount(dto.getDiscount());
        entity.setAmountDiscount(dto.isAmountDiscount());
        entity.setTerms(dto.getTerms());
        entity.setPublicNotes(dto.getPublicNotes());
        entity.setPrivateNotes(dto.getPrivateNotes());
        entity.setFooter(dto.getFooter());

        if (dto.getLineItems() != null) {
            entity.getLineItems().clear();
            populateLineItemsAndCalculate(entity, dto.getLineItems());
        }

        QuoteEntity updated = quoteRepository.save(entity);
        return toDto(updated);
    }

    @Transactional
    public InvoiceDto convertToInvoice(UUID quoteId) {
        UUID companyId = getRequiredCompanyId();
        QuoteEntity quote = quoteRepository.findByCompanyIdAndId(companyId, quoteId)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found: " + quoteId));

        String nextInvoiceNumber = sequenceGeneratorService.generateSequence("invoice_" + companyId);

        InvoiceEntity invoice = InvoiceEntity.builder()
                .companyId(companyId)
                .clientId(quote.getClientId())
                .userId(quote.getUserId())
                .assignedUserId(quote.getAssignedUserId())
                .number(nextInvoiceNumber)
                .poNumber(quote.getPoNumber())
                .date(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .statusId("draft")
                .amount(quote.getAmount())
                .balance(quote.getAmount())
                .paidToDate(BigDecimal.ZERO)
                .discount(quote.getDiscount())
                .isAmountDiscount(quote.isAmountDiscount())
                .totalTaxes(quote.getTotalTaxes())
                .terms(quote.getTerms())
                .publicNotes(quote.getPublicNotes())
                .privateNotes(quote.getPrivateNotes())
                .footer(quote.getFooter())
                .usesInclusiveTaxes(quote.isUsesInclusiveTaxes())
                .taxName1(quote.getTaxName1())
                .taxRate1(quote.getTaxRate1())
                .taxName2(quote.getTaxName2())
                .taxRate2(quote.getTaxRate2())
                .isDeleted(false)
                .lineItems(new ArrayList<>())
                .build();

        for (QuoteItemEntity qi : quote.getLineItems()) {
            InvoiceItemEntity item = InvoiceItemEntity.builder()
                    .productKey(qi.getProductKey())
                    .notes(qi.getNotes())
                    .cost(qi.getCost())
                    .quantity(qi.getQuantity())
                    .discount(qi.getDiscount())
                    .isAmountDiscount(qi.isAmountDiscount())
                    .lineTotal(qi.getLineTotal())
                    .grossLineTotal(qi.getLineTotal())
                    .taxName1(qi.getTaxName1())
                    .taxRate1(qi.getTaxRate1())
                    .sortId(qi.getSortId())
                    .build();
            invoice.addLineItem(item);
        }

        InvoiceEntity savedInvoice = invoiceRepository.save(invoice);

        quote.setStatusId("converted");
        quote.setInvoiceId(savedInvoice.getId());
        quoteRepository.save(quote);

        clientRepository.findByCompanyIdAndId(companyId, quote.getClientId()).ifPresent(c -> {
            c.setBalance(c.getBalance().add(savedInvoice.getBalance()));
            clientRepository.save(c);
        });

        return invoiceService.toDto(savedInvoice);
    }

    @Transactional
    public List<QuoteDto> bulkAction(QuoteBulkActionRequest request) {
        UUID companyId = getRequiredCompanyId();
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return Collections.emptyList();
        }

        List<QuoteEntity> entities = quoteRepository.findByCompanyIdAndIdIn(companyId, request.getIds());
        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";

        List<QuoteEntity> resultEntities;

        switch (action) {
            case "archive":
                Instant now = Instant.now();
                for (QuoteEntity q : entities) {
                    q.setArchivedAt(now);
                    q.setDeleted(false);
                }
                resultEntities = quoteRepository.saveAll(entities);
                break;

            case "restore":
                for (QuoteEntity q : entities) {
                    q.setArchivedAt(null);
                    q.setDeleted(false);
                }
                resultEntities = quoteRepository.saveAll(entities);
                break;

            case "delete":
                Instant delTime = Instant.now();
                for (QuoteEntity q : entities) {
                    q.setDeleted(true);
                    q.setArchivedAt(delTime);
                }
                resultEntities = quoteRepository.saveAll(entities);
                break;

            case "approve":
                for (QuoteEntity q : entities) {
                    q.setStatusId("approved");
                }
                resultEntities = quoteRepository.saveAll(entities);
                break;

            default:
                resultEntities = entities;
                break;
        }

        return resultEntities.stream().map(this::toDto).collect(Collectors.toList());
    }

    private void populateLineItemsAndCalculate(QuoteEntity entity, List<QuoteItemDto> itemDtos) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalLineTaxes = BigDecimal.ZERO;

        if (itemDtos != null && !itemDtos.isEmpty()) {
            int sort = 0;
            for (QuoteItemDto itemDto : itemDtos) {
                BigDecimal qty = itemDto.getQuantity() != null ? itemDto.getQuantity() : BigDecimal.ONE;
                BigDecimal cost = itemDto.getCost() != null ? itemDto.getCost() : BigDecimal.ZERO;
                BigDecimal rawLine = qty.multiply(cost);

                BigDecimal discount = itemDto.getDiscount() != null ? itemDto.getDiscount() : BigDecimal.ZERO;
                BigDecimal lineDiscAmount = itemDto.isAmountDiscount()
                        ? discount
                        : rawLine.multiply(discount).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

                BigDecimal netLineTotal = rawLine.subtract(lineDiscAmount).max(BigDecimal.ZERO);

                BigDecimal t1 = itemDto.getTaxRate1() != null ? itemDto.getTaxRate1() : BigDecimal.ZERO;
                BigDecimal lineTaxAmount = netLineTotal.multiply(t1).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

                subtotal = subtotal.add(netLineTotal);
                totalLineTaxes = totalLineTaxes.add(lineTaxAmount);

                QuoteItemEntity itemEntity = QuoteItemEntity.builder()
                        .productKey(itemDto.getProductKey())
                        .notes(itemDto.getNotes())
                        .cost(cost)
                        .quantity(qty)
                        .discount(discount)
                        .isAmountDiscount(itemDto.isAmountDiscount())
                        .lineTotal(netLineTotal)
                        .taxName1(itemDto.getTaxName1())
                        .taxRate1(t1)
                        .sortId(sort++)
                        .build();

                entity.addLineItem(itemEntity);
            }
        }

        BigDecimal quoteDiscount = entity.getDiscount() != null ? entity.getDiscount() : BigDecimal.ZERO;
        BigDecimal quoteDiscAmount = entity.isAmountDiscount()
                ? quoteDiscount
                : subtotal.multiply(quoteDiscount).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal discountedSubtotal = subtotal.subtract(quoteDiscAmount).max(BigDecimal.ZERO);

        BigDecimal t1 = entity.getTaxRate1() != null ? entity.getTaxRate1() : BigDecimal.ZERO;
        BigDecimal t2 = entity.getTaxRate2() != null ? entity.getTaxRate2() : BigDecimal.ZERO;
        BigDecimal quoteTaxAmount = discountedSubtotal.multiply(t1.add(t2)).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal totalTaxes = totalLineTaxes.add(quoteTaxAmount);
        BigDecimal totalAmount = discountedSubtotal.add(totalTaxes);

        entity.setTotalTaxes(totalTaxes);
        entity.setAmount(totalAmount);
        entity.setBalance(totalAmount);
    }

    public QuoteDto toDto(QuoteEntity entity) {
        if (entity == null) {
            return null;
        }

        List<QuoteItemDto> itemDtos = entity.getLineItems() != null
                ? entity.getLineItems().stream().map(this::toItemDto).collect(Collectors.toList())
                : Collections.emptyList();

        return QuoteDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .clientId(entity.getClientId())
                .invoiceId(entity.getInvoiceId())
                .number(entity.getNumber())
                .poNumber(entity.getPoNumber())
                .date(entity.getDate() != null ? entity.getDate().toString() : null)
                .dueDate(entity.getDueDate() != null ? entity.getDueDate().toString() : null)
                .statusId(entity.getStatusId())
                .amount(entity.getAmount())
                .balance(entity.getBalance())
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
                .customValue1(entity.getCustomValue1())
                .customValue2(entity.getCustomValue2())
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .lineItems(itemDtos)
                .invitations(Collections.emptyList())
                .documents(Collections.emptyList())
                .build();
    }

    private QuoteItemDto toItemDto(QuoteItemEntity item) {
        return QuoteItemDto.builder()
                .id(item.getId())
                .productKey(item.getProductKey())
                .notes(item.getNotes())
                .cost(item.getCost())
                .quantity(item.getQuantity())
                .discount(item.getDiscount())
                .isAmountDiscount(item.isAmountDiscount())
                .lineTotal(item.getLineTotal())
                .taxName1(item.getTaxName1())
                .taxRate1(item.getTaxRate1())
                .sortId(item.getSortId())
                .build();
    }

    private LocalDate parseDate(String dateStr, LocalDate fallback) {
        if (!StringUtils.hasText(dateStr)) return fallback;
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            return fallback;
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
