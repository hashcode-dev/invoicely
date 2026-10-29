package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.RecurringInvoiceDto;
import com.hashcode.invoicely.entity.relational.ClientEntity;
import com.hashcode.invoicely.entity.relational.RecurringInvoiceEntity;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.RecurringInvoiceRepository;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringInvoiceService {

    private final RecurringInvoiceRepository recurringInvoiceRepository;
    private final ClientRepository clientRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Transactional(readOnly = true)
    public GenericManyResponse<RecurringInvoiceDto> getRecurringInvoices(int page, int perPage, String status, String filter) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 20;

        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Specification<RecurringInvoiceEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));

            if ("archived".equalsIgnoreCase(status)) {
                predicates.add(cb.isNotNull(root.get("archivedAt")));
                predicates.add(cb.isFalse(root.get("isDeleted")));
            } else if ("deleted".equalsIgnoreCase(status)) {
                predicates.add(cb.isTrue(root.get("isDeleted")));
            } else if (!"all".equalsIgnoreCase(status)) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
                predicates.add(cb.isNull(root.get("archivedAt")));
            }

            if (StringUtils.hasText(filter)) {
                predicates.add(cb.like(cb.lower(root.get("number")), "%" + filter.trim().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<RecurringInvoiceEntity> resultPage = recurringInvoiceRepository.findAll(spec, pageable);
        List<RecurringInvoiceDto> dtos = resultPage.getContent().stream().map(this::toDto).collect(Collectors.toList());

        return GenericManyResponse.of(dtos, resultPage.getTotalElements(), page, pageSize);
    }

    @Transactional(readOnly = true)
    public RecurringInvoiceDto getBlankRecurringInvoice() {
        UUID companyId = getRequiredCompanyId();
        String nextNumber = sequenceGeneratorService.generateSequence("rec_inv_" + companyId);

        return RecurringInvoiceDto.builder()
                .number(nextNumber)
                .frequencyId("monthly")
                .statusId("active")
                .autoBill(false)
                .amount(BigDecimal.ZERO)
                .remainingCycles(-1)
                .nextSendDate(LocalDate.now().plusMonths(1).toString())
                .build();
    }

    @Transactional(readOnly = true)
    public RecurringInvoiceDto getRecurringInvoiceById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        RecurringInvoiceEntity entity = recurringInvoiceRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Recurring invoice not found: " + id));
        return toDto(entity);
    }

    @Transactional
    public RecurringInvoiceDto createRecurringInvoice(RecurringInvoiceDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        if (dto.getClientId() == null) {
            throw new IllegalArgumentException("client_id is required to create a recurring invoice");
        }

        ClientEntity client = clientRepository.findByCompanyIdAndId(companyId, dto.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + dto.getClientId()));

        String number = StringUtils.hasText(dto.getNumber())
                ? dto.getNumber().trim()
                : sequenceGeneratorService.generateSequence("rec_inv_" + companyId);

        LocalDate nextSendDate = parseDate(dto.getNextSendDate(), LocalDate.now().plusMonths(1));

        RecurringInvoiceEntity entity = RecurringInvoiceEntity.builder()
                .companyId(companyId)
                .clientId(client.getId())
                .userId(userId)
                .number(number)
                .frequencyId(StringUtils.hasText(dto.getFrequencyId()) ? dto.getFrequencyId() : "monthly")
                .statusId(StringUtils.hasText(dto.getStatusId()) ? dto.getStatusId() : "active")
                .autoBill(dto.isAutoBill())
                .amount(dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO)
                .remainingCycles(dto.getRemainingCycles() != null ? dto.getRemainingCycles() : -1)
                .nextSendDate(nextSendDate)
                .isDeleted(false)
                .build();

        RecurringInvoiceEntity saved = recurringInvoiceRepository.save(entity);
        return toDto(saved);
    }

    public RecurringInvoiceDto toDto(RecurringInvoiceEntity entity) {
        if (entity == null) {
            return null;
        }

        return RecurringInvoiceDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .clientId(entity.getClientId())
                .number(entity.getNumber())
                .frequencyId(entity.getFrequencyId())
                .statusId(entity.getStatusId())
                .autoBill(entity.isAutoBill())
                .amount(entity.getAmount())
                .remainingCycles(entity.getRemainingCycles())
                .nextSendDate(entity.getNextSendDate() != null ? entity.getNextSendDate().toString() : null)
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
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
