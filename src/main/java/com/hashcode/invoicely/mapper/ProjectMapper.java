package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.client.ClientDto;
import com.hashcode.invoicely.dto.project.ProjectDto;
import com.hashcode.invoicely.entity.relational.ProjectEntity;
import com.hashcode.invoicely.entity.relational.TaskEntity;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectMapper {

    private final ClientRepository clientRepository;
    private final TaskRepository taskRepository;
    private final ClientMapper clientMapper;

    public ProjectDto toDto(ProjectEntity entity) {
        if (entity == null) {
            return null;
        }

        ClientDto clientDto = null;
        if (entity.getClientId() != null) {
            try {
                clientDto = clientRepository.findById(entity.getClientId()).map(clientMapper::toDto).orElse(null);
            } catch (Exception e) {
                log.debug("Client {} not found for project {}", entity.getClientId(), entity.getId());
            }
        }

        BigDecimal currentHours = calculateProjectHours(entity.getCompanyId(), entity.getId());
        return toDto(entity, currentHours, clientDto);
    }

    public ProjectDto toDto(ProjectEntity entity, BigDecimal currentHours, ClientDto clientDto) {
        if (entity == null) {
            return null;
        }
        return ProjectDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .clientId(entity.getClientId())
                .name(entity.getName())
                .number(entity.getNumber())
                .taskRate(entity.getTaskRate())
                .budgetedHours(entity.getBudgetedHours())
                .currentHours(currentHours)
                .dueDate(entity.getDueDate())
                .privateNotes(entity.getPrivateNotes())
                .publicNotes(entity.getPublicNotes())
                .color(entity.getColor())
                .customValue1(entity.getCustomValue1())
                .customValue2(entity.getCustomValue2())
                .customValue3(entity.getCustomValue3())
                .customValue4(entity.getCustomValue4())
                .isDeleted(Boolean.TRUE.equals(entity.getIsDeleted()))
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .client(clientDto)
                .build();
    }

    private BigDecimal calculateProjectHours(UUID companyId, UUID projectId) {
        if (companyId == null || projectId == null) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        List<TaskEntity> tasks = taskRepository.findByCompanyIdAndProjectIdAndIsDeletedFalse(companyId, projectId);
        long totalSeconds = 0L;
        for (TaskEntity task : tasks) {
            if (task.getDuration() != null) {
                totalSeconds += task.getDuration();
            }
        }
        return BigDecimal.valueOf(totalSeconds)
                .divide(BigDecimal.valueOf(3600), 3, RoundingMode.HALF_UP);
    }
}
