package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.document.BlueprintDto;
import com.hashcode.invoicely.entity.relational.BlueprintEntity;
import org.springframework.stereotype.Component;

@Component
public class BlueprintMapper {

    public BlueprintDto toDto(BlueprintEntity entity) {
        if (entity == null) {
            return null;
        }
        return BlueprintDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .content(entity.getContent())
                .isTemplate(Boolean.TRUE.equals(entity.getIsTemplate()))
                .isDeleted(Boolean.TRUE.equals(entity.getIsDeleted()))
                .clientId(entity.getClientId())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .build();
    }
}
