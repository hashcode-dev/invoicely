package com.hashcode.invoicely.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.dto.document.DesignDto;
import com.hashcode.invoicely.dto.document.PartsDto;
import com.hashcode.invoicely.entity.relational.DesignEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class DesignMapper {

    private final ObjectMapper objectMapper;

    public DesignDto toDto(DesignEntity entity) {
        if (entity == null) {
            return null;
        }
        return DesignDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .design(parseParts(entity.getDesign()))
                .isCustom(Boolean.TRUE.equals(entity.getIsCustom()))
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .isDeleted(Boolean.TRUE.equals(entity.getIsDeleted()))
                .isFree(Boolean.TRUE.equals(entity.getIsFree()))
                .isTemplate(Boolean.TRUE.equals(entity.getIsTemplate()))
                .entities(entity.getEntities())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .build();
    }

    public PartsDto parseParts(String json) {
        if (!StringUtils.hasText(json)) {
            return PartsDto.builder().build();
        }
        try {
            return objectMapper.readValue(json, PartsDto.class);
        } catch (Exception e) {
            return PartsDto.builder().build();
        }
    }

    public String serializeParts(PartsDto parts) {
        if (parts == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(parts);
        } catch (Exception e) {
            return "{}";
        }
    }
}
