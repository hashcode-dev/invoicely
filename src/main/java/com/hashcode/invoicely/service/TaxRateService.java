package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.invoice.TaxRateDto;
import com.hashcode.invoicely.entity.relational.TaxRateEntity;
import com.hashcode.invoicely.repository.relational.TaxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxRateService {

    private final TaxRateRepository taxRateRepository;

    @Transactional(readOnly = true)
    public GenericManyResponse<TaxRateDto> getTaxRates() {
        UUID companyId = getRequiredCompanyId();
        List<TaxRateEntity> entities = taxRateRepository.findByCompanyIdAndIsDeletedFalse(companyId);
        List<TaxRateDto> dtos = entities.stream().map(this::toDto).collect(Collectors.toList());
        return GenericManyResponse.of(dtos, dtos.size(), 1, Math.max(dtos.size(), 1));
    }

    @Transactional(readOnly = true)
    public TaxRateDto getTaxRateById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        TaxRateEntity entity = taxRateRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Tax rate not found: " + id));
        return toDto(entity);
    }

    @Transactional
    public TaxRateDto createTaxRate(TaxRateDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        TaxRateEntity entity = TaxRateEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .name(dto.getName() != null ? dto.getName().trim() : "Tax")
                .rate(dto.getRate() != null ? dto.getRate() : BigDecimal.ZERO)
                .isInclusive(dto.isInclusive())
                .isDeleted(false)
                .build();

        TaxRateEntity saved = taxRateRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public TaxRateDto updateTaxRate(UUID id, TaxRateDto dto) {
        UUID companyId = getRequiredCompanyId();
        TaxRateEntity entity = taxRateRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Tax rate not found: " + id));

        if (dto.getName() != null) {
            entity.setName(dto.getName().trim());
        }
        if (dto.getRate() != null) {
            entity.setRate(dto.getRate());
        }
        entity.setInclusive(dto.isInclusive());

        TaxRateEntity updated = taxRateRepository.save(entity);
        return toDto(updated);
    }

    @Transactional
    public void deleteTaxRate(UUID id) {
        UUID companyId = getRequiredCompanyId();
        TaxRateEntity entity = taxRateRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Tax rate not found: " + id));
        entity.setDeleted(true);
        entity.setArchivedAt(Instant.now());
        taxRateRepository.save(entity);
    }

    public TaxRateDto toDto(TaxRateEntity entity) {
        if (entity == null) {
            return null;
        }
        return TaxRateDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .rate(entity.getRate())
                .isInclusive(entity.isInclusive())
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .build();
    }

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalArgumentException("Tenant company context is missing");
        }
        return companyId;
    }
}
