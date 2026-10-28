package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.project.TaskStatusDto;
import com.hashcode.invoicely.service.TaskStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/task_statuses")
@RequiredArgsConstructor
public class TaskStatusController {

    private final TaskStatusService taskStatusService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<TaskStatusDto>> getTaskStatuses(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "100") int perPage,
            @RequestParam(name = "status", defaultValue = "active") String status,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "sort", required = false) String sort) {
        return ResponseEntity.ok(taskStatusService.getTaskStatuses(page, perPage, status, filter, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskStatusDto>> getTaskStatusById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(taskStatusService.getTaskStatusById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskStatusDto>> createTaskStatus(@RequestBody TaskStatusDto dto) {
        TaskStatusDto created = taskStatusService.createTaskStatus(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskStatusDto>> updateTaskStatus(
            @PathVariable UUID id,
            @RequestBody TaskStatusDto dto) {
        TaskStatusDto updated = taskStatusService.updateTaskStatus(id, dto);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTaskStatus(@PathVariable UUID id) {
        taskStatusService.deleteTaskStatus(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }
}
