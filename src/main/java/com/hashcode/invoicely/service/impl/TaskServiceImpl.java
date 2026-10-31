package com.hashcode.invoicely.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.project.TaskBulkActionRequest;
import com.hashcode.invoicely.dto.project.TaskDto;
import com.hashcode.invoicely.dto.project.TaskSortRequest;
import com.hashcode.invoicely.entity.relational.ClientEntity;
import com.hashcode.invoicely.entity.relational.InvoiceEntity;
import com.hashcode.invoicely.entity.relational.InvoiceItemEntity;
import com.hashcode.invoicely.entity.relational.ProjectEntity;
import com.hashcode.invoicely.entity.relational.TaskEntity;
import com.hashcode.invoicely.mapper.TaskMapper;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.InvoiceRepository;
import com.hashcode.invoicely.repository.relational.ProjectRepository;
import com.hashcode.invoicely.repository.relational.TaskRepository;
import com.hashcode.invoicely.repository.relational.TaskStatusRepository;
import com.hashcode.invoicely.service.SequenceGeneratorService;
import com.hashcode.invoicely.service.TaskService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final TaskMapper taskMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public GenericManyResponse<TaskDto> getTasks(int page, int perPage, String status, UUID projectId, UUID clientId, UUID statusId, UUID projectTasks, String filter, String sort) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 50;

        Sort sorting = Sort.by(Sort.Direction.DESC, "createdAt");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split("\\|");
            if (parts.length == 2) {
                sorting = Sort.by("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);

        final UUID targetProjectId = projectId != null ? projectId : projectTasks;

        Specification<TaskEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));

            if (targetProjectId != null) {
                predicates.add(cb.equal(root.get("projectId"), targetProjectId));
            }
            if (clientId != null) {
                predicates.add(cb.equal(root.get("clientId"), clientId));
            }
            if (statusId != null) {
                predicates.add(cb.equal(root.get("statusId"), statusId));
            }

            if ("archived".equalsIgnoreCase(status)) {
                predicates.add(cb.isNotNull(root.get("archivedAt")));
                predicates.add(cb.isFalse(root.get("isDeleted")));
            } else if ("deleted".equalsIgnoreCase(status)) {
                predicates.add(cb.isTrue(root.get("isDeleted")));
            } else if (!"all".equalsIgnoreCase(status)) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
                predicates.add(cb.isNull(root.get("archivedAt")));
            }

            if (StringUtils.hasText(filter)) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + filter.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<TaskEntity> pageResult = taskRepository.findAll(spec, pageable);
        List<TaskDto> dtos = pageResult.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return GenericManyResponse.of(dtos, pageResult.getTotalElements(), pageResult.getTotalPages(), pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTaskById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        TaskEntity entity = taskRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getBlankTask() {
        return TaskDto.builder()
                .duration(0L)
                .isRunning(false)
                .timeLog("[]")
                .rate(BigDecimal.ZERO)
                .isDeleted(false)
                .date(LocalDate.now())
                .build();
    }

    @Override
    @Transactional
    public TaskDto createTask(TaskDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        String taskNumber = StringUtils.hasText(dto.getNumber())
                ? dto.getNumber().trim()
                : sequenceGeneratorService.generateSequence("task_" + companyId);

        UUID resolvedClientId = dto.getClientId();
        if (resolvedClientId == null && dto.getProjectId() != null) {
            projectRepository.findByCompanyIdAndId(companyId, dto.getProjectId())
                    .ifPresent(p -> {
                        if (p.getClientId() != null) {
                            dto.setClientId(p.getClientId());
                        }
                    });
            resolvedClientId = dto.getClientId();
        }

        BigDecimal rate = dto.getRate();
        if ((rate == null || rate.compareTo(BigDecimal.ZERO) == 0) && dto.getProjectId() != null) {
            rate = projectRepository.findByCompanyIdAndId(companyId, dto.getProjectId())
                    .map(ProjectEntity::getTaskRate)
                    .orElse(BigDecimal.ZERO);
        }
        if (rate == null) {
            rate = BigDecimal.ZERO;
        }

        String initialTimeLog = StringUtils.hasText(dto.getTimeLog()) ? dto.getTimeLog() : "[]";
        long duration = dto.getDuration() != null ? dto.getDuration() : calculateDurationFromTimeLog(initialTimeLog);

        TaskEntity entity = TaskEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .assignedUserId(dto.getAssignedUserId())
                .clientId(resolvedClientId)
                .projectId(dto.getProjectId())
                .statusId(dto.getStatusId())
                .number(taskNumber)
                .description(dto.getDescription())
                .duration(duration)
                .isRunning(dto.isRunning())
                .timeLog(initialTimeLog)
                .rate(rate)
                .isDateBased(dto.isDateBased())
                .date(dto.getDate() != null ? dto.getDate() : LocalDate.now())
                .calculatedStartDate(dto.getCalculatedStartDate())
                .statusOrder(dto.getStatusOrder() != null ? dto.getStatusOrder() : 0)
                .statusSortOrder(dto.getStatusSortOrder() != null ? dto.getStatusSortOrder() : 0)
                .customValue1(dto.getCustomValue1())
                .customValue2(dto.getCustomValue2())
                .customValue3(dto.getCustomValue3())
                .customValue4(dto.getCustomValue4())
                .isDeleted(false)
                .build();

        TaskEntity saved = taskRepository.save(entity);
        log.info("Created task {} ({}) for company {}", saved.getNumber(), saved.getId(), companyId);
        return toDto(saved);
    }

    @Override
    @Transactional
    public TaskDto updateTask(UUID id, TaskDto dto, boolean start, boolean stop) {
        UUID companyId = getRequiredCompanyId();
        TaskEntity entity = taskRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));

        if (start) {
            startTimer(entity);
        } else if (stop) {
            stopTimer(entity);
        } else {
            if (dto.getDescription() != null) {
                entity.setDescription(dto.getDescription());
            }
            if (dto.getClientId() != null) {
                entity.setClientId(dto.getClientId());
            }
            if (dto.getProjectId() != null) {
                entity.setProjectId(dto.getProjectId());
            }
            if (dto.getStatusId() != null) {
                entity.setStatusId(dto.getStatusId());
            }
            if (dto.getAssignedUserId() != null) {
                entity.setAssignedUserId(dto.getAssignedUserId());
            }
            if (dto.getRate() != null) {
                entity.setRate(dto.getRate());
            }
            if (dto.getDate() != null) {
                entity.setDate(dto.getDate());
            }
            if (StringUtils.hasText(dto.getTimeLog())) {
                entity.setTimeLog(dto.getTimeLog());
                entity.setDuration(calculateDurationFromTimeLog(dto.getTimeLog()));
            } else if (dto.getDuration() != null) {
                entity.setDuration(dto.getDuration());
            }
            if (dto.getStatusOrder() != null) {
                entity.setStatusOrder(dto.getStatusOrder());
            }
            if (dto.getStatusSortOrder() != null) {
                entity.setStatusSortOrder(dto.getStatusSortOrder());
            }
            if (dto.getCustomValue1() != null) {
                entity.setCustomValue1(dto.getCustomValue1());
            }
            if (dto.getCustomValue2() != null) {
                entity.setCustomValue2(dto.getCustomValue2());
            }
            if (dto.getCustomValue3() != null) {
                entity.setCustomValue3(dto.getCustomValue3());
            }
            if (dto.getCustomValue4() != null) {
                entity.setCustomValue4(dto.getCustomValue4());
            }
        }

        TaskEntity saved = taskRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteTask(UUID id) {
        UUID companyId = getRequiredCompanyId();
        TaskEntity entity = taskRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        entity.setIsDeleted(true);
        taskRepository.save(entity);
        log.info("Soft deleted task {} for company {}", id, companyId);
    }

    @Override
    @Transactional
    public void sortTasks(TaskSortRequest request) {
        if (request == null) return;
        UUID companyId = getRequiredCompanyId();

        if (request.getStatusIds() != null) {
            for (int i = 0; i < request.getStatusIds().size(); i++) {
                final int orderIndex = i;
                UUID statusId = request.getStatusIds().get(i);
                taskStatusRepository.findByCompanyIdAndId(companyId, statusId).ifPresent(status -> {
                    status.setSortOrder(orderIndex);
                    status.setStatusOrder(orderIndex);
                    taskStatusRepository.save(status);
                });
            }
        }

        if (request.getTaskIds() != null) {
            for (Map.Entry<String, List<UUID>> entry : request.getTaskIds().entrySet()) {
                try {
                    UUID statusId = UUID.fromString(entry.getKey());
                    List<UUID> taskIds = entry.getValue();
                    if (taskIds != null) {
                        for (int i = 0; i < taskIds.size(); i++) {
                            final int orderIndex = i;
                            UUID taskId = taskIds.get(i);
                            taskRepository.findByCompanyIdAndId(companyId, taskId).ifPresent(task -> {
                                task.setStatusId(statusId);
                                task.setStatusOrder(orderIndex);
                                task.setStatusSortOrder(orderIndex);
                                taskRepository.save(task);
                            });
                        }
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid UUID for statusId key in taskIds map: {}", entry.getKey());
                }
            }
        }
    }

    @Override
    @Transactional
    public Map<String, Object> handleBulkAction(TaskBulkActionRequest request) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            return Map.of("processed", 0);
        }

        List<TaskEntity> tasks = taskRepository.findByCompanyIdAndIdIn(companyId, request.getIds());
        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";

        switch (action) {
            case "archive" -> {
                for (TaskEntity task : tasks) {
                    task.setArchivedAt(Instant.now());
                    task.setIsDeleted(false);
                }
                taskRepository.saveAll(tasks);
                return Map.of("processed", tasks.size());
            }
            case "restore" -> {
                for (TaskEntity task : tasks) {
                    task.setArchivedAt(null);
                    task.setIsDeleted(false);
                }
                taskRepository.saveAll(tasks);
                return Map.of("processed", tasks.size());
            }
            case "delete" -> {
                for (TaskEntity task : tasks) {
                    task.setIsDeleted(true);
                }
                taskRepository.saveAll(tasks);
                return Map.of("processed", tasks.size());
            }
            case "invoice_tasks", "invoice" -> {
                List<TaskEntity> invoiceableTasks = tasks.stream()
                        .filter(t -> t.getInvoiceId() == null && !Boolean.TRUE.equals(t.getIsDeleted()))
                        .collect(Collectors.toList());

                if (invoiceableTasks.isEmpty()) {
                    return Map.of("processed", 0, "message", "No uninvoiced tasks selected");
                }

                UUID clientId = invoiceableTasks.stream()
                        .map(TaskEntity::getClientId)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Tasks must be assigned to a client to generate an invoice"));

                ClientEntity client = clientRepository.findByCompanyIdAndId(companyId, clientId)
                        .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

                String invNumber = sequenceGeneratorService.generateSequence("invoice_" + companyId);
                InvoiceEntity invoice = InvoiceEntity.builder()
                        .companyId(companyId)
                        .clientId(client.getId())
                        .userId(userId)
                        .number(invNumber)
                        .date(LocalDate.now())
                        .dueDate(LocalDate.now().plusDays(30))
                        .statusId("draft")
                        .amount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                        .balance(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                        .paidToDate(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                        .isDeleted(false)
                        .build();

                BigDecimal totalInvoiceAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
                int order = 0;

                for (TaskEntity task : invoiceableTasks) {
                    BigDecimal taskHours = calculateHours(task.getDuration());
                    BigDecimal taskRate = task.getRate() != null && task.getRate().compareTo(BigDecimal.ZERO) > 0
                            ? task.getRate()
                            : BigDecimal.valueOf(50.0).setScale(4, RoundingMode.HALF_UP);

                    BigDecimal lineTotal = taskHours.multiply(taskRate).setScale(4, RoundingMode.HALF_UP);

                    InvoiceItemEntity item = InvoiceItemEntity.builder()
                            .productKey("TASK")
                            .notes("Task #" + (task.getNumber() != null ? task.getNumber() : "") + (StringUtils.hasText(task.getDescription()) ? ": " + task.getDescription() : ""))
                            .cost(taskRate)
                            .quantity(taskHours)
                            .lineTotal(lineTotal)
                            .grossLineTotal(lineTotal)
                            .typeId("3")
                            .sortId(order++)
                            .build();

                    invoice.addLineItem(item);
                    totalInvoiceAmount = totalInvoiceAmount.add(lineTotal);
                }

                invoice.setAmount(totalInvoiceAmount);
                invoice.setBalance(totalInvoiceAmount);
                InvoiceEntity savedInvoice = invoiceRepository.save(invoice);

                for (TaskEntity task : invoiceableTasks) {
                    task.setInvoiceId(savedInvoice.getId());
                }
                taskRepository.saveAll(invoiceableTasks);

                client.setBalance(client.getBalance().add(totalInvoiceAmount));
                clientRepository.save(client);

                log.info("Converted {} tasks into Invoice {} for client {}", invoiceableTasks.size(), savedInvoice.getNumber(), client.getId());
                return Map.of("processed", invoiceableTasks.size(), "invoice_id", savedInvoice.getId(), "invoice_number", savedInvoice.getNumber());
            }
            default -> {
                log.warn("Unknown task bulk action: {}", action);
                return Map.of("processed", 0);
            }
        }
    }

    @Override
    public TaskDto toDto(TaskEntity entity) {
        return taskMapper.toDto(entity);
    }

    private void startTimer(TaskEntity entity) {
        if (Boolean.TRUE.equals(entity.getIsRunning())) {
            log.info("Task {} is already running timer", entity.getId());
            return;
        }

        List<List<Object>> logs = parseTimeLogList(entity.getTimeLog());
        long now = Instant.now().getEpochSecond();
        List<Object> newInterval = new ArrayList<>();
        newInterval.add(now);
        newInterval.add(0);

        logs.add(newInterval);
        entity.setTimeLog(serializeTimeLog(logs));
        entity.setIsRunning(true);
        if (entity.getCalculatedStartDate() == null) {
            entity.setCalculatedStartDate(Instant.now());
        }
        log.info("Started timer for task {}", entity.getId());
    }

    private void stopTimer(TaskEntity entity) {
        if (!Boolean.TRUE.equals(entity.getIsRunning())) {
            log.info("Task {} timer is not running", entity.getId());
            return;
        }

        List<List<Object>> logs = parseTimeLogList(entity.getTimeLog());
        long now = Instant.now().getEpochSecond();

        if (!logs.isEmpty()) {
            List<Object> lastInterval = logs.get(logs.size() - 1);
            if (lastInterval.size() >= 2) {
                lastInterval.set(1, now);
            }
        }

        long totalDuration = calculateDurationFromLogs(logs);
        entity.setDuration(totalDuration);
        entity.setTimeLog(serializeTimeLog(logs));
        entity.setIsRunning(false);
        log.info("Stopped timer for task {}. New duration: {}s", entity.getId(), totalDuration);
    }

    private List<List<Object>> parseTimeLogList(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<List<Object>>>() {});
        } catch (Exception e) {
            log.warn("Could not parse time_log JSON: '{}', resetting to empty list", json);
            return new ArrayList<>();
        }
    }

    private String serializeTimeLog(List<List<Object>> logs) {
        try {
            return objectMapper.writeValueAsString(logs);
        } catch (Exception e) {
            log.error("Failed to serialize time_log", e);
            return "[]";
        }
    }

    private long calculateDurationFromTimeLog(String json) {
        List<List<Object>> logs = parseTimeLogList(json);
        return calculateDurationFromLogs(logs);
    }

    private long calculateDurationFromLogs(List<List<Object>> logs) {
        long sum = 0L;
        for (List<Object> interval : logs) {
            if (interval != null && interval.size() >= 2) {
                long start = interval.get(0) instanceof Number ? ((Number) interval.get(0)).longValue() : 0L;
                long stop = interval.get(1) instanceof Number ? ((Number) interval.get(1)).longValue() : 0L;
                if (stop > start) {
                    sum += (stop - start);
                }
            }
        }
        return sum;
    }

    private BigDecimal calculateHours(Long durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(durationSeconds)
                .divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);
    }

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("TenantContext missing company ID");
        }
        return companyId;
    }
}
