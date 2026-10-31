package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.project.TaskBulkActionRequest;
import com.hashcode.invoicely.dto.project.TaskDto;
import com.hashcode.invoicely.dto.project.TaskSortRequest;
import com.hashcode.invoicely.entity.relational.TaskEntity;

import java.util.Map;
import java.util.UUID;

public interface TaskService {

    GenericManyResponse<TaskDto> getTasks(int page, int perPage, String status, UUID projectId, UUID clientId, UUID statusId, UUID projectTasks, String filter, String sort);

    TaskDto getTaskById(UUID id);

    TaskDto getBlankTask();

    TaskDto createTask(TaskDto dto);

    TaskDto updateTask(UUID id, TaskDto dto, boolean start, boolean stop);

    void deleteTask(UUID id);

    void sortTasks(TaskSortRequest request);

    Map<String, Object> handleBulkAction(TaskBulkActionRequest request);

    TaskDto toDto(TaskEntity entity);
}
