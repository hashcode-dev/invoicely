package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.invoice.TaxRateDto;
import com.hashcode.invoicely.entity.relational.TaxRateEntity;
import org.springframework.stereotype.Component;

@Component
public class TaxRateMapper {

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
}
