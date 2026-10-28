package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.banking.BankAccountDto;
import com.hashcode.invoicely.dto.banking.BankIntegrationBulkActionRequest;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.entity.relational.BankIntegrationEntity;
import com.hashcode.invoicely.repository.relational.BankIntegrationRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankIntegrationService {

    private final BankIntegrationRepository bankIntegrationRepository;

    @Transactional(readOnly = true)
    public GenericManyResponse<BankAccountDto> getBankIntegrations(int page, int perPage, String status, String filter, String sort) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 50;

        Sort sorting = Sort.by(Sort.Direction.DESC, "createdAt");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split("\\|");
            if (parts.length == 2) {
                sorting = Sort.by("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);

        Specification<BankIntegrationEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));

            if ("archived".equalsIgnoreCase(status)) {
                predicates.add(cb.isNotNull(root.get("archivedAt")));
                predicates.add(cb.isFalse(root.get("isDeleted")));
            } else if ("deleted".equalsIgnoreCase(status)) {
                predicates.add(cb.isTrue(root.get("isDeleted")));
            } else if ("all".equalsIgnoreCase(status)) {
                // all
            } else {
                predicates.add(cb.isFalse(root.get("isDeleted")));
                predicates.add(cb.isNull(root.get("archivedAt")));
            }

            if (StringUtils.hasText(filter)) {
                predicates.add(cb.like(cb.lower(root.get("bankAccountName")), "%" + filter.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<BankIntegrationEntity> pageResult = bankIntegrationRepository.findAll(spec, pageable);
        List<BankAccountDto> dtos = pageResult.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return GenericManyResponse.of(dtos, pageResult.getTotalElements(), pageResult.getTotalPages(), pageSize);
    }

    @Transactional(readOnly = true)
    public BankAccountDto getBankIntegrationById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        BankIntegrationEntity entity = bankIntegrationRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Bank integration not found: " + id));
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public BankAccountDto getBlankBankIntegration() {
        return BankAccountDto.builder()
                .bankAccountType("checking")
                .bankAccountStatus("active")
                .currency("USD")
                .balance(BigDecimal.ZERO)
                .autoSync(true)
                .disabledUpstream(false)
                .isDeleted(false)
                .fromDate(LocalDate.now())
                .build();
    }

    @Transactional
    public BankAccountDto createBankIntegration(BankAccountDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        String accountName = StringUtils.hasText(dto.getBankAccountName())
                ? dto.getBankAccountName().trim()
                : "Bank Account";

        BankIntegrationEntity entity = BankIntegrationEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .bankAccountName(accountName)
                .bankAccountNumber(dto.getBankAccountNumber())
                .bankAccountType(StringUtils.hasText(dto.getBankAccountType()) ? dto.getBankAccountType() : "checking")
                .bankAccountStatus(StringUtils.hasText(dto.getBankAccountStatus()) ? dto.getBankAccountStatus() : "active")
                .currency(StringUtils.hasText(dto.getCurrency()) ? dto.getCurrency() : "USD")
                .balance(dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO)
                .nickname(dto.getNickname())
                .providerName(StringUtils.hasText(dto.getProviderName()) ? dto.getProviderName() : "manual")
                .integrationType(StringUtils.hasText(dto.getIntegrationType()) ? dto.getIntegrationType() : "plaid")
                .nordigenInstitutionId(dto.getNordigenInstitutionId())
                .autoSync(dto.isAutoSync())
                .disabledUpstream(dto.isDisabledUpstream())
                .fromDate(dto.getFromDate())
                .isDeleted(false)
                .build();

        BankIntegrationEntity saved = bankIntegrationRepository.save(entity);
        log.info("Created bank integration {} ({}) for company {}", saved.getBankAccountName(), saved.getId(), companyId);
        return toDto(saved);
    }

    @Transactional
    public BankAccountDto updateBankIntegration(UUID id, BankAccountDto dto) {
        UUID companyId = getRequiredCompanyId();
        BankIntegrationEntity entity = bankIntegrationRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Bank integration not found: " + id));

        if (StringUtils.hasText(dto.getBankAccountName())) {
            entity.setBankAccountName(dto.getBankAccountName());
        }
        if (dto.getBankAccountNumber() != null) {
            entity.setBankAccountNumber(dto.getBankAccountNumber());
        }
        if (dto.getBankAccountType() != null) {
            entity.setBankAccountType(dto.getBankAccountType());
        }
        if (dto.getBankAccountStatus() != null) {
            entity.setBankAccountStatus(dto.getBankAccountStatus());
        }
        if (dto.getCurrency() != null) {
            entity.setCurrency(dto.getCurrency());
        }
        if (dto.getBalance() != null) {
            entity.setBalance(dto.getBalance());
        }
        if (dto.getNickname() != null) {
            entity.setNickname(dto.getNickname());
        }
        if (dto.getProviderName() != null) {
            entity.setProviderName(dto.getProviderName());
        }
        if (dto.getIntegrationType() != null) {
            entity.setIntegrationType(dto.getIntegrationType());
        }
        if (dto.getNordigenInstitutionId() != null) {
            entity.setNordigenInstitutionId(dto.getNordigenInstitutionId());
        }
        entity.setAutoSync(dto.isAutoSync());
        entity.setDisabledUpstream(dto.isDisabledUpstream());
        if (dto.getFromDate() != null) {
            entity.setFromDate(dto.getFromDate());
        }

        BankIntegrationEntity saved = bankIntegrationRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public void deleteBankIntegration(UUID id) {
        UUID companyId = getRequiredCompanyId();
        BankIntegrationEntity entity = bankIntegrationRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Bank integration not found: " + id));
        entity.setIsDeleted(true);
        bankIntegrationRepository.save(entity);
        log.info("Soft deleted bank integration {} for company {}", id, companyId);
    }

    @Transactional
    public int handleBulkAction(BankIntegrationBulkActionRequest request) {
        UUID companyId = getRequiredCompanyId();
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            return 0;
        }

        List<BankIntegrationEntity> entities = bankIntegrationRepository.findByCompanyIdAndIdIn(companyId, request.getIds());
        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";

        for (BankIntegrationEntity entity : entities) {
            switch (action) {
                case "archive" -> {
                    entity.setArchivedAt(Instant.now());
                    entity.setIsDeleted(false);
                }
                case "restore" -> {
                    entity.setArchivedAt(null);
                    entity.setIsDeleted(false);
                }
                case "delete" -> entity.setIsDeleted(true);
                default -> log.warn("Unknown bank integration bulk action: {}", action);
            }
        }

        bankIntegrationRepository.saveAll(entities);
        return entities.size();
    }

    @Transactional
    public List<BankAccountDto> refreshAccounts() {
        UUID companyId = getRequiredCompanyId();
        List<BankIntegrationEntity> entities = bankIntegrationRepository.findByCompanyIdAndIsDeletedFalse(companyId);
        log.info("Refreshed {} bank accounts for company {}", entities.size(), companyId);
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    public BankAccountDto toDto(BankIntegrationEntity entity) {
        if (entity == null) return null;
        return BankAccountDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .bankAccountName(entity.getBankAccountName())
                .bankAccountNumber(entity.getBankAccountNumber())
                .bankAccountType(entity.getBankAccountType())
                .bankAccountStatus(entity.getBankAccountStatus())
                .currency(entity.getCurrency())
                .balance(entity.getBalance())
                .nickname(entity.getNickname())
                .providerName(entity.getProviderName())
                .integrationType(entity.getIntegrationType())
                .nordigenInstitutionId(entity.getNordigenInstitutionId())
                .autoSync(Boolean.TRUE.equals(entity.getAutoSync()))
                .disabledUpstream(Boolean.TRUE.equals(entity.getDisabledUpstream()))
                .fromDate(entity.getFromDate())
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
