package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.project.TaskStatusDto;
import com.hashcode.invoicely.entity.relational.TaskStatusEntity;

import java.util.UUID;

public interface TaskStatusService {

    GenericManyResponse<TaskStatusDto> getTaskStatuses(int page, int perPage, String status, String filter, String sort);

    TaskStatusDto getTaskStatusById(UUID id);

    TaskStatusDto createTaskStatus(TaskStatusDto dto);

    TaskStatusDto updateTaskStatus(UUID id, TaskStatusDto dto);

    void deleteTaskStatus(UUID id);

    TaskStatusDto toDto(TaskStatusEntity entity);
}
