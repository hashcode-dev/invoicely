package com.hashcode.invoicely.service.impl;

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
import com.hashcode.invoicely.mapper.InvoiceMapper;
import com.hashcode.invoicely.mapper.QuoteMapper;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.InvoiceRepository;
import com.hashcode.invoicely.repository.relational.QuoteRepository;
import com.hashcode.invoicely.service.ClientService;
import com.hashcode.invoicely.service.QuoteService;
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
public class QuoteServiceImpl implements QuoteService {

    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final ClientService clientService;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final QuoteMapper quoteMapper;
    private final InvoiceMapper invoiceMapper;

    @Override
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
                String pattern = "%" + filter.trim().toLowerCase() + "%";
                Predicate numMatch = cb.like(cb.lower(root.get("number")), pattern);
                Predicate poMatch = cb.like(cb.lower(root.get("poNumber")), pattern);
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

    @Override
    @Transactional(readOnly = true)
    public QuoteDto getBlankQuote(UUID clientId) {
        UUID companyId = getRequiredCompanyId();
        String nextNumber = sequenceGeneratorService.generateSequence("quote_" + companyId);

        QuoteItemDto defaultItem = QuoteItemDto.builder()
                .quantity(BigDecimal.ONE)
                .cost(BigDecimal.ZERO)
                .lineTotal(BigDecimal.ZERO)
                .build();

        QuoteDto.QuoteDtoBuilder builder = QuoteDto.builder()
                .number(nextNumber)
                .date(LocalDate.now().toString())
                .dueDate(LocalDate.now().plusDays(30).toString())
                .statusId("draft")
                .amount(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
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
    public QuoteDto getQuoteById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        QuoteEntity entity = quoteRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found with id: " + id));
        return toDto(entity);
    }

    @Override
    @Transactional
    public QuoteDto createQuote(QuoteDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        if (dto.getClientId() == null) {
            throw new IllegalArgumentException("Client ID is required to create a quote");
        }

        ClientEntity client = clientRepository.findByCompanyIdAndId(companyId, dto.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + dto.getClientId()));

        String quoteNumber = StringUtils.hasText(dto.getNumber())
                ? dto.getNumber().trim()
                : sequenceGeneratorService.generateSequence("quote_" + companyId);

        LocalDate quoteDate = parseDate(dto.getDate(), LocalDate.now());
        LocalDate dueDate = parseDate(dto.getDueDate(), quoteDate.plusDays(30));

        QuoteEntity entity = QuoteEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .assignedUserId(dto.getAssignedUserId())
                .clientId(client.getId())
                .number(quoteNumber)
                .poNumber(dto.getPoNumber())
                .date(quoteDate)
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

    @Override
    @Transactional
    public QuoteDto updateQuote(UUID id, QuoteDto dto) {
        UUID companyId = getRequiredCompanyId();
        QuoteEntity entity = quoteRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found with id: " + id));

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
        if (dto.getTerms() != null) entity.setTerms(dto.getTerms());
        if (dto.getPublicNotes() != null) entity.setPublicNotes(dto.getPublicNotes());
        if (dto.getPrivateNotes() != null) entity.setPrivateNotes(dto.getPrivateNotes());
        if (dto.getFooter() != null) entity.setFooter(dto.getFooter());
        entity.setUsesInclusiveTaxes(dto.isUsesInclusiveTaxes());

        if (dto.getTaxName1() != null) entity.setTaxName1(dto.getTaxName1());
        if (dto.getTaxRate1() != null) entity.setTaxRate1(dto.getTaxRate1());
        if (dto.getTaxName2() != null) entity.setTaxName2(dto.getTaxName2());
        if (dto.getTaxRate2() != null) entity.setTaxRate2(dto.getTaxRate2());

        if (dto.getCustomValue1() != null) entity.setCustomValue1(dto.getCustomValue1());
        if (dto.getCustomValue2() != null) entity.setCustomValue2(dto.getCustomValue2());

        if (dto.getLineItems() != null) {
            entity.getLineItems().clear();
            populateLineItemsAndCalculate(entity, dto.getLineItems());
        }

        QuoteEntity updated = quoteRepository.save(entity);
        return toDto(updated);
    }

    @Override
    @Transactional
    public InvoiceDto convertToInvoice(UUID quoteId) {
        UUID companyId = getRequiredCompanyId();
        QuoteEntity quote = quoteRepository.findByCompanyIdAndId(companyId, quoteId)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found with id: " + quoteId));

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

        return invoiceMapper.toDto(savedInvoice);
    }

    @Override
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

                QuoteItemEntity item = QuoteItemEntity.builder()
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

                entity.addLineItem(item);
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

    @Override
    public QuoteDto toDto(QuoteEntity entity) {
        return quoteMapper.toDto(entity);
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
