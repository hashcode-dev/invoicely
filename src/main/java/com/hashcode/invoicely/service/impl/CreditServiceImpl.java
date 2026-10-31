package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.CreditDto;
import com.hashcode.invoicely.entity.relational.ClientEntity;
import com.hashcode.invoicely.entity.relational.CreditEntity;
import com.hashcode.invoicely.mapper.CreditMapper;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.CreditRepository;
import com.hashcode.invoicely.service.CreditService;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final CreditRepository creditRepository;
    private final ClientRepository clientRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final CreditMapper creditMapper;

    @Override
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
                String pattern = "%" + filter.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("number")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CreditEntity> creditPage = creditRepository.findAll(spec, pageable);
        List<CreditDto> dtos = creditPage.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return GenericManyResponse.of(dtos, creditPage.getTotalElements(), page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public CreditDto getBlankCredit(UUID clientId) {
        UUID companyId = getRequiredCompanyId();
        String nextNumber = sequenceGeneratorService.generateSequence("credit_" + companyId);

        return CreditDto.builder()
                .number(nextNumber)
                .clientId(clientId)
                .date(LocalDate.now().toString())
                .dueDate(LocalDate.now().plusDays(30).toString())
                .statusId("draft")
                .amount(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CreditDto getCreditById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        CreditEntity entity = creditRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Credit not found with id: " + id));
        return toDto(entity);
    }

    @Override
    @Transactional
    public CreditDto createCredit(CreditDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        if (dto.getClientId() == null) {
            throw new IllegalArgumentException("Client ID is required to create a credit note");
        }

        ClientEntity client = clientRepository.findByCompanyIdAndId(companyId, dto.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + dto.getClientId()));

        String creditNumber = StringUtils.hasText(dto.getNumber())
                ? dto.getNumber().trim()
                : sequenceGeneratorService.generateSequence("credit_" + companyId);

        BigDecimal amount = dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO;

        CreditEntity entity = CreditEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .clientId(client.getId())
                .invoiceId(dto.getInvoiceId())
                .number(creditNumber)
                .date(parseDate(dto.getDate(), LocalDate.now()))
                .dueDate(parseDate(dto.getDueDate(), LocalDate.now().plusDays(30)))
                .amount(amount)
                .balance(amount)
                .statusId(StringUtils.hasText(dto.getStatusId()) ? dto.getStatusId() : "draft")
                .publicNotes(dto.getPublicNotes())
                .privateNotes(dto.getPrivateNotes())
                .terms(dto.getTerms())
                .isDeleted(false)
                .build();

        client.setCreditBalance(client.getCreditBalance().add(amount));
        clientRepository.save(client);

        CreditEntity saved = creditRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public CreditDto updateCredit(UUID id, CreditDto dto) {
        UUID companyId = getRequiredCompanyId();
        CreditEntity entity = creditRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Credit not found with id: " + id));

        BigDecimal oldBalance = entity.getBalance();

        if (StringUtils.hasText(dto.getNumber())) entity.setNumber(dto.getNumber().trim());
        if (dto.getAmount() != null) {
            entity.setAmount(dto.getAmount());
            entity.setBalance(dto.getAmount());
        }
        if (dto.getStatusId() != null) entity.setStatusId(dto.getStatusId());
        if (dto.getPublicNotes() != null) entity.setPublicNotes(dto.getPublicNotes());
        if (dto.getPrivateNotes() != null) entity.setPrivateNotes(dto.getPrivateNotes());
        if (dto.getTerms() != null) entity.setTerms(dto.getTerms());

        CreditEntity updated = creditRepository.save(entity);

        BigDecimal diff = entity.getBalance().subtract(oldBalance);
        if (diff.compareTo(BigDecimal.ZERO) != 0) {
            clientRepository.findByCompanyIdAndId(companyId, entity.getClientId()).ifPresent(client -> {
                client.setCreditBalance(client.getCreditBalance().add(diff));
                clientRepository.save(client);
            });
        }

        return toDto(updated);
    }

    @Override
    public CreditDto toDto(CreditEntity entity) {
        return creditMapper.toDto(entity);
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
