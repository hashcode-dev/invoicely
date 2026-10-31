package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.client.ClientDto;
import com.hashcode.invoicely.dto.project.ProjectDto;
import com.hashcode.invoicely.dto.project.TaskDto;
import com.hashcode.invoicely.dto.project.TaskStatusDto;
import com.hashcode.invoicely.entity.relational.TaskEntity;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.ProjectRepository;
import com.hashcode.invoicely.repository.relational.TaskStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskMapper {

    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final ClientMapper clientMapper;
    private final ProjectMapper projectMapper;
    private final TaskStatusMapper taskStatusMapper;

    public TaskDto toDto(TaskEntity entity) {
        if (entity == null) {
            return null;
        }

        ClientDto clientDto = null;
        if (entity.getClientId() != null) {
            try {
                clientDto = clientRepository.findById(entity.getClientId()).map(clientMapper::toDto).orElse(null);
            } catch (Exception e) {
                log.debug("Client {} not found for task {}", entity.getClientId(), entity.getId());
            }
        }

        ProjectDto projectDto = null;
        if (entity.getProjectId() != null) {
            try {
                projectDto = projectRepository.findById(entity.getProjectId()).map(projectMapper::toDto).orElse(null);
            } catch (Exception e) {
                log.debug("Project {} not found for task {}", entity.getProjectId(), entity.getId());
            }
        }

        TaskStatusDto statusDto = null;
        if (entity.getStatusId() != null) {
            try {
                statusDto = taskStatusRepository.findById(entity.getStatusId()).map(taskStatusMapper::toDto).orElse(null);
            } catch (Exception e) {
                log.debug("Status {} not found for task {}", entity.getStatusId(), entity.getId());
            }
        }

        return toDto(entity, clientDto, projectDto, statusDto);
    }

    public TaskDto toDto(TaskEntity entity, ClientDto clientDto, ProjectDto projectDto, TaskStatusDto statusDto) {
        if (entity == null) {
            return null;
        }
        return TaskDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .clientId(entity.getClientId())
                .projectId(entity.getProjectId())
                .invoiceId(entity.getInvoiceId())
                .statusId(entity.getStatusId())
                .number(entity.getNumber())
                .description(entity.getDescription())
                .duration(entity.getDuration())
                .isRunning(Boolean.TRUE.equals(entity.getIsRunning()))
                .timeLog(entity.getTimeLog())
                .rate(entity.getRate())
                .isDateBased(Boolean.TRUE.equals(entity.getIsDateBased()))
                .date(entity.getDate())
                .calculatedStartDate(entity.getCalculatedStartDate())
                .statusOrder(entity.getStatusOrder())
                .statusSortOrder(entity.getStatusSortOrder())
                .customValue1(entity.getCustomValue1())
                .customValue2(entity.getCustomValue2())
                .customValue3(entity.getCustomValue3())
                .customValue4(entity.getCustomValue4())
                .isDeleted(Boolean.TRUE.equals(entity.getIsDeleted()))
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .client(clientDto)
                .project(projectDto)
                .status(statusDto)
                .build();
    }
}
