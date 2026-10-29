package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.CreditDto;
import com.hashcode.invoicely.entity.relational.ClientEntity;
import com.hashcode.invoicely.entity.relational.CreditEntity;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.CreditRepository;
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
public class CreditService {

    private final CreditRepository creditRepository;
    private final ClientRepository clientRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Transactional(readOnly = true)
    public GenericManyResponse<CreditDto> getCredits(int page, int perPage, String status, UUID clientId, String filter) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 20;

        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Specification<CreditEntity> spec = (root, query, cb) -> {
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
            } else if (!"all".equalsIgnoreCase(status)) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
                predicates.add(cb.isNull(root.get("archivedAt")));
            }

            if (StringUtils.hasText(filter)) {
                predicates.add(cb.like(cb.lower(root.get("number")), "%" + filter.trim().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CreditEntity> creditPage = creditRepository.findAll(spec, pageable);
        List<CreditDto> dtos = creditPage.getContent().stream().map(this::toDto).collect(Collectors.toList());

        return GenericManyResponse.of(dtos, creditPage.getTotalElements(), page, pageSize);
    }

    @Transactional(readOnly = true)
    public CreditDto getBlankCredit(UUID clientId) {
        UUID companyId = getRequiredCompanyId();
        String nextNumber = sequenceGeneratorService.generateSequence("credit_" + companyId);

        return CreditDto.builder()
                .number(nextNumber)
                .clientId(clientId)
                .date(LocalDate.now().toString())
                .statusId("draft")
                .amount(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .build();
    }

    @Transactional(readOnly = true)
    public CreditDto getCreditById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        CreditEntity entity = creditRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Credit note not found: " + id));
        return toDto(entity);
    }

    @Transactional
    public CreditDto createCredit(CreditDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        if (dto.getClientId() == null) {
            throw new IllegalArgumentException("client_id is required to create a credit note");
        }

        ClientEntity client = clientRepository.findByCompanyIdAndId(companyId, dto.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + dto.getClientId()));

        String creditNumber = StringUtils.hasText(dto.getNumber())
                ? dto.getNumber().trim()
                : sequenceGeneratorService.generateSequence("credit_" + companyId);

        LocalDate date = parseDate(dto.getDate(), LocalDate.now());
        BigDecimal amount = dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO;

        CreditEntity entity = CreditEntity.builder()
                .companyId(companyId)
                .clientId(client.getId())
                .userId(userId)
                .invoiceId(dto.getInvoiceId())
                .number(creditNumber)
                .date(date)
                .amount(amount)
                .balance(amount)
                .statusId(StringUtils.hasText(dto.getStatusId()) ? dto.getStatusId() : "draft")
                .publicNotes(dto.getPublicNotes())
                .privateNotes(dto.getPrivateNotes())
                .terms(dto.getTerms())
                .isDeleted(false)
                .build();

        CreditEntity saved = creditRepository.save(entity);

        client.setCreditBalance(client.getCreditBalance().add(amount));
        clientRepository.save(client);

        return toDto(saved);
    }

    @Transactional
    public CreditDto updateCredit(UUID id, CreditDto dto) {
        UUID companyId = getRequiredCompanyId();
        CreditEntity entity = creditRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Credit note not found: " + id));

        BigDecimal oldAmount = entity.getAmount();

        if (StringUtils.hasText(dto.getNumber())) entity.setNumber(dto.getNumber().trim());
        if (StringUtils.hasText(dto.getDate())) entity.setDate(parseDate(dto.getDate(), entity.getDate()));
        if (StringUtils.hasText(dto.getStatusId())) entity.setStatusId(dto.getStatusId());
        if (dto.getAmount() != null) {
            entity.setAmount(dto.getAmount());
            entity.setBalance(dto.getAmount());
        }
        entity.setPublicNotes(dto.getPublicNotes());
        entity.setPrivateNotes(dto.getPrivateNotes());
        entity.setTerms(dto.getTerms());

        CreditEntity updated = creditRepository.save(entity);

        BigDecimal diff = updated.getAmount().subtract(oldAmount);
        if (diff.compareTo(BigDecimal.ZERO) != 0) {
            clientRepository.findByCompanyIdAndId(companyId, updated.getClientId()).ifPresent(c -> {
                c.setCreditBalance(c.getCreditBalance().add(diff));
                clientRepository.save(c);
            });
        }

        return toDto(updated);
    }

    public CreditDto toDto(CreditEntity entity) {
        if (entity == null) {
            return null;
        }

        return CreditDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .clientId(entity.getClientId())
                .invoiceId(entity.getInvoiceId())
                .number(entity.getNumber())
                .date(entity.getDate() != null ? entity.getDate().toString() : null)
                .dueDate(entity.getDueDate() != null ? entity.getDueDate().toString() : null)
                .amount(entity.getAmount())
                .balance(entity.getBalance())
                .statusId(entity.getStatusId())
                .publicNotes(entity.getPublicNotes())
                .privateNotes(entity.getPrivateNotes())
                .terms(entity.getTerms())
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
