package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.project.TaskBulkActionRequest;
import com.hashcode.invoicely.dto.project.TaskDto;
import com.hashcode.invoicely.dto.project.TaskSortRequest;
import com.hashcode.invoicely.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<TaskDto>> getTasks(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "50") int perPage,
            @RequestParam(name = "status", defaultValue = "active") String status,
            @RequestParam(name = "project_id", required = false) UUID projectId,
            @RequestParam(name = "client_id", required = false) UUID clientId,
            @RequestParam(name = "status_id", required = false) UUID statusId,
            @RequestParam(name = "project_tasks", required = false) UUID projectTasks,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "sort", required = false) String sort) {
        return ResponseEntity.ok(taskService.getTasks(page, perPage, status, projectId, clientId, statusId, projectTasks, filter, sort));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<TaskDto>> getBlankTask() {
        return ResponseEntity.ok(ApiResponse.of(taskService.getBlankTask()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDto>> getTaskById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(taskService.getTaskById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskDto>> createTask(@RequestBody TaskDto dto) {
        TaskDto created = taskService.createTask(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDto>> updateTask(
            @PathVariable UUID id,
            @RequestParam(name = "start", required = false, defaultValue = "false") boolean start,
            @RequestParam(name = "stop", required = false, defaultValue = "false") boolean stop,
            @RequestBody(required = false) TaskDto dto) {
        TaskDto updated = taskService.updateTask(id, dto != null ? dto : new TaskDto(), start, stop);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable UUID id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/sort")
    public ResponseEntity<Map<String, Object>> sortTasks(@RequestBody TaskSortRequest request) {
        taskService.sortTasks(request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> handleBulkAction(@RequestBody TaskBulkActionRequest request) {
        Map<String, Object> result = taskService.handleBulkAction(request);
        return ResponseEntity.ok(result);
    }
}
