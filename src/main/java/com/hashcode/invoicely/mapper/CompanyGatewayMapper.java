package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.payment.CompanyGatewayDto;
import com.hashcode.invoicely.entity.relational.CompanyGatewayEntity;
import org.springframework.stereotype.Component;

@Component
public class CompanyGatewayMapper {

    public CompanyGatewayDto toDto(CompanyGatewayEntity entity) {
        if (entity == null) {
            return null;
        }
        return CompanyGatewayDto.builder()
                .id(entity.getId())
                .gatewayKey(entity.getGatewayKey())
                .label(entity.getLabel())
                .config(entity.getConfig())
                .isDeleted(entity.isDeleted())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .build();
    }
}
