package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.project.TaskStatusDto;
import com.hashcode.invoicely.entity.relational.TaskStatusEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskStatusMapper {

    public TaskStatusDto toDto(TaskStatusEntity entity) {
        if (entity == null) {
            return null;
        }
        return TaskStatusDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .color(entity.getColor())
                .sortOrder(entity.getSortOrder())
                .statusOrder(entity.getStatusOrder())
                .isDeleted(Boolean.TRUE.equals(entity.getIsDeleted()))
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .build();
    }
}
